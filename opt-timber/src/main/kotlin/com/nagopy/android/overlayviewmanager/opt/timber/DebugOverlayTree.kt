/*
 * Copyright 2017 75py
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nagopy.android.overlayviewmanager.opt.timber

import android.app.Activity
import android.app.Application
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.TextView
import androidx.annotation.MainThread
import com.nagopy.android.overlayviewmanager.OverlayResult
import com.nagopy.android.overlayviewmanager.OverlaySpec
import com.nagopy.android.overlayviewmanager.OverlayState
import com.nagopy.android.overlayviewmanager.OverlayTouchMode
import com.nagopy.android.overlayviewmanager.OverlayView
import com.nagopy.android.overlayviewmanager.OverlayViewManager
import com.nagopy.android.overlayviewmanager.internal.Logger
import com.nagopy.android.overlayviewmanager.internal.SimpleActivityLifecycleCallbacks
import com.nagopy.android.overlayviewmanager.internal.WeakReferenceCache
import java.util.ArrayDeque
import timber.log.Timber

/**
 * [Option] Implementation of [Timber.Tree].
 */
open class DebugOverlayTree private constructor() : Timber.DebugTree() {

    /**
     * Guards [messages], [maxLines], [generation] and [pendingRenderGeneration] so that [log]
     * (called from arbitrary threads) and [setMaxLines], [initialize] and [dispose] (all
     * main-thread) never observe or produce a torn buffer, and so a queued [render] can detect
     * that it was scheduled for a generation this tree has since left.
     */
    private val bufferLock = Any()

    /**
     * Read from the logging thread by [postToMainThread]. Safe without
     * additional locking because it is assigned only once, in this private,
     * never-reassigned field initializer, which runs while the singleton
     * instance is created by the class's static initializer; the JLS
     * guarantees that write is visible to every thread that subsequently
     * observes an initialized [DebugOverlayTree] class (JLS 12.4.2).
     */
    private val mainHandler: Handler = Handler(Looper.getMainLooper())

    /** Null when uninitialized (never [initialize]d, or after a successful [dispose]). */
    private var messages: ArrayDeque<String>? = null

    /**
     * Incremented by [initialize] and by a successful [dispose]. Lets a render [Runnable] queued
     * for one lifecycle generation detect, when it finally runs, that [dispose] and a subsequent
     * re-[initialize] have since happened and skip rendering stale messages into the new
     * generation's overlay. Guarded by [bufferLock].
     */
    private var generation: Int = 0

    /**
     * [NO_PENDING_RENDER] when no render is currently pending; otherwise the [generation] a
     * pending render was scheduled for. Guarded by [bufferLock].
     *
     * Tracking this as "the generation a pending render targets" rather than a plain boolean is
     * what lets [log] correctly re-arm scheduling across a [dispose] + re-[initialize] that
     * happens before an already-queued render [Runnable] drains: a stale render belonging to a
     * generation this tree has since left is recognized as such by [render] (and, since it never
     * claims the *current* generation's slot, never blocks a fresh [log] in the new generation
     * from scheduling its own render). A single shared pending/not-pending flag would instead let
     * that stale Runnable's completion silently clear the new generation's pending state before
     * it renders, losing the fresh message until some later, unrelated [log] call happened to
     * schedule again.
     */
    private var pendingRenderGeneration: Int = NO_PENDING_RENDER

    /**
     * Written by [initialize] (called once from [init], on whichever thread
     * the caller's `Application.onCreate` runs on) and read by
     * [getInstance], [register], [render], and the
     * [activityLifecycleCallbacks] methods (`onActivityStarted` /
     * `onActivityStopped`, invoked by the platform on the main thread).
     * [render] itself only ever runs on the main thread because it is
     * always invoked through [postToMainThread]. [initialize], [register],
     * [dispose] and [getInstance] are ordinary methods, not Android-enforced
     * main-thread entry points; the documented usage calls them from
     * application/main-thread code (`Application.onCreate`,
     * `Activity.onCreate`), and this class does not defend against calling
     * them concurrently with each other. What this field genuinely needs
     * protecting from is [log], which runs on whatever thread Timber is
     * logging from: [log] never reads or writes this field directly, so the
     * logging thread can never observe a torn or stale reference here.
     */
    private var overlayView: OverlayView<TextView>? = null

    /**
     * The [Application] this tree is initialized for. `null` when uninitialized. Used by
     * [initializeIfNeeded] to recognize a same-Application re-[init] as a no-op and reject a
     * different-Application [init] while already initialized. Cleared by a successful [dispose].
     */
    private var application: Application? = null

    private var registeredActivities: WeakReferenceCache<Activity>? = null

    private var runningActivities: WeakReferenceCache<Activity>? = null

    private var registeredAndRunningActivities: WeakReferenceCache<Activity>? = null

    /**
     * Minimum log priority accepted by [log]. [setThreshold] is typically
     * called from the main thread while [log] runs on whatever thread
     * Timber is logging from, so there is no lock or other happens-before
     * edge naturally connecting the two. This field is `volatile` rather
     * than guarded by [bufferLock] so the threshold check in [log] stays
     * lock-free on its fast path (the common case where a line is
     * filtered out before the buffer is ever touched).
     */
    @Volatile
    private var threshold: Int = 0

    /** Guarded by [bufferLock] together with [messages] so [initialize] resets both atomically. */
    private var maxLines: Int = 0

    companion object {

        private const val DEFAULT_MAX_LINES: Int = 5

        /** Sentinel for [pendingRenderGeneration]: no render is currently pending. */
        private const val NO_PENDING_RENDER: Int = -1

        private var INSTANCE: DebugOverlayTree = DebugOverlayTree()

        /**
         * Initialize and return the [Timber.Tree] implementation.
         * Usage: `Timber.plant(DebugOverlayTree.init(this /* Application */));`
         *
         * Calling this again with the same [application] is a no-op that returns the already-live
         * instance without re-registering lifecycle callbacks or creating another overlay view.
         * Calling it with a *different* [Application] while already initialized throws
         * [IllegalStateException] and leaves the live instance untouched. Calling it again after a
         * successful [dispose] re-initializes cleanly, including with the same [Application].
         *
         * @param application Your [Application] instance
         * @return The [Timber.Tree] implementation
         * @throws IllegalStateException if already initialized for a different [Application]
         */
        @JvmStatic
        fun init(application: Application): DebugOverlayTree {
            INSTANCE.initializeIfNeeded(application)
            return INSTANCE
        }

        /**
         * Return the [DebugOverlayTree] instance. The instance is singleton.
         *
         * @return instance
         * @throws IllegalStateException if never initialized, or if [dispose] has since released it
         */
        @JvmStatic
        fun getInstance(): DebugOverlayTree {
            if (INSTANCE.overlayView == null) {
                throw IllegalStateException(
                    "DebugOverlayTree is not initialized. Please call init(Application) first.",
                )
            }
            return INSTANCE
        }
    }

    init {
        overlayView = null
    }

    /**
     * Decides whether [application] requires a fresh [initialize]: a same-Application re-init is
     * a harmless no-op, a different Application while initialized throws without mutating any
     * state, and re-init after [dispose] (when [DebugOverlayTree.application] is `null`) always
     * initializes cleanly.
     */
    private fun initializeIfNeeded(application: Application) {
        val current = this.application
        if (current != null) {
            check(current === application) {
                "DebugOverlayTree is already initialized for a different Application."
            }
            return
        }
        initialize(application)
    }

    /**
     * Inner method. Initialize members.
     *
     * @param application Application
     */
    private fun initialize(application: Application) {
        val view = TextView(application)
        val handle = OverlayViewManager.getInstance().newOverlayView(
            view,
            OverlaySpec(
                alpha = 0.4f,
                gravity = Gravity.BOTTOM,
                width = MATCH_PARENT,
                touchMode = OverlayTouchMode.PASS_THROUGH,
            ),
        )
        view.setTextColor(Color.WHITE)
        view.setBackgroundColor(Color.BLACK)

        // messages and maxLines reset together, atomically: a background log() racing this
        // initialize() must never populate the new buffer while still observing the prior
        // (possibly larger) maxLines limit.
        synchronized(bufferLock) {
            messages = ArrayDeque()
            maxLines = DEFAULT_MAX_LINES
            generation++
        }
        threshold = Log.DEBUG
        overlayView = handle
        registeredActivities = WeakReferenceCache()
        runningActivities = WeakReferenceCache()
        registeredAndRunningActivities = WeakReferenceCache()
        this.application = application
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
    }

    /**
     * Releases the overlay handle, lifecycle callbacks, message buffer and Activity caches,
     * returning this tree to the uninitialized state ([getInstance] throws again afterward).
     *
     * Transactional: this first disposes the overlay handle. If that fails, this returns the
     * failure unchanged and leaves the handle, callbacks,
     * caches and buffer untouched so the host can retry by calling [dispose] again -- dropping the
     * only handle on a failed disposal would leak an attached window with no way to retry. Only a
     * successful handle disposal unregisters the lifecycle callbacks and clears everything.
     *
     * Idempotent: calling this again after a successful disposal is a no-op that returns a
     * successful result with `changed == false` and `state == `[OverlayState.DISPOSED], never an
     * exception. `Timber.uproot(tree)` remains the host's own responsibility; this does not call
     * it.
     *
     * [log] becomes a silent no-op, and a render already queued before this call becomes a no-op,
     * only once this successfully completes -- not merely once it is called.
     *
     * @return the result of disposing the underlying overlay handle
     * @throws IllegalStateException if not called on the main thread
     */
    @MainThread
    open fun dispose(): OverlayResult {
        requireMainThread()
        val handle = overlayView ?: return OverlayResult(OverlayState.DISPOSED, false, null, null)
        val result = handle.dispose()
        if (!result.isSuccess) {
            logFailure(result, "dispose")
            return result
        }
        application?.unregisterActivityLifecycleCallbacks(activityLifecycleCallbacks)
        synchronized(bufferLock) {
            messages = null
            generation++
        }
        overlayView = null
        registeredActivities = null
        runningActivities = null
        registeredAndRunningActivities = null
        application = null
        return result
    }

    /**
     * Set minimum log level.
     *
     * @param threshold Log level
     */
    open fun setThreshold(threshold: Int) {
        this.threshold = threshold
    }

    /**
     * Set the maximum number of retained log lines shown in the overlay.
     *
     * Shrinking below the current retained line count trims the oldest
     * lines immediately and re-renders the overlay; growing takes effect
     * for subsequent log lines without touching the current buffer.
     *
     * @param maxLines Max line number, must be 1 or greater
     * @throws IllegalArgumentException if [maxLines] is less than 1
     */
    open fun setMaxLines(maxLines: Int) {
        if (maxLines < 1) {
            throw IllegalArgumentException("maxLines must be >= 1, but was $maxLines")
        }
        var shouldPost = false
        synchronized(bufferLock) {
            this.maxLines = maxLines
            if (trimLocked() && pendingRenderGeneration != generation) {
                pendingRenderGeneration = generation
                shouldPost = true
            }
        }
        if (shouldPost) {
            postToMainThread(renderRunnable)
        }
    }

    /**
     * Register the activity. If called, the debug view is displayed while the activity is visible.
     *
     * @param activity Activity
     */
    open fun register(activity: Activity) {
        registeredActivities!!.add(activity)
        if (runningActivities!!.contains(activity)) {
            registeredAndRunningActivities!!.add(activity)
            showOverlay()
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < threshold) {
            return
        }

        var shouldPost = false
        synchronized(bufferLock) {
            val currentMessages = messages ?: return@synchronized
            currentMessages.addLast("$tag: $message")
            trimLocked()
            // A pending render already targeting this generation will pick up this line when it
            // runs (render() reads the live buffer, not a snapshot); only schedule a new one if
            // none is currently pending for the CURRENT generation -- in particular, a render left
            // over from a generation this tree has since left through dispose()+init() does not
            // count, so this line is never lost waiting on a stale Runnable to drain.
            if (pendingRenderGeneration != generation) {
                pendingRenderGeneration = generation
                shouldPost = true
            }
        }
        if (shouldPost) {
            postToMainThread(renderRunnable)
        }
    }

    /**
     * Drop the oldest lines until [messages] fits within [maxLines]. A no-op, rather than a crash,
     * once [messages] is `null` (uninitialized or disposed). Callers must hold [bufferLock].
     *
     * @return true if at least one line was dropped
     */
    private fun trimLocked(): Boolean {
        val currentMessages = messages ?: return false
        var removed = false
        while (currentMessages.size > maxLines) {
            currentMessages.removeFirst()
            removed = true
        }
        return removed
    }

    /**
     * A single stateless closure, reused for every post: it carries no generation of its own and
     * always reads the live [generation]/[pendingRenderGeneration]/[messages] at the moment it
     * actually runs, never a value captured at schedule time. This is why coalescing two posts of
     * the SAME object (e.g. one queued before a dispose+re-init, one queued after) is safe: which
     * of the two dequeued invocations happens to observe [generation] `==` [pendingRenderGeneration]
     * -- and therefore performs the render -- is irrelevant, because [render] always renders the
     * CURRENT buffer, not whatever buffer existed when that particular post was made. Exactly one
     * of any pair of posts renders (the first to reach [render] while the match still holds); the
     * other is a no-op because the first already cleared [pendingRenderGeneration].
     */
    private val renderRunnable: Runnable = Runnable { render() }

    /**
     * Build the text for the current buffer and apply it to the TextView. Only ever invoked on
     * the main thread via [renderRunnable]. A no-op if [messages] is `null` (uninitialized or
     * disposed since this render was scheduled) or if [generation] no longer matches
     * [pendingRenderGeneration] (a dispose and re-init happened in between, or another invocation
     * of the same shared [renderRunnable] already serviced this generation): either way this must
     * never render stale messages into a new lifecycle generation's overlay, and must never render
     * the same generation twice for one schedule.
     */
    private fun render() {
        var text: String? = null
        var view: TextView? = null
        synchronized(bufferLock) {
            val currentMessages = messages ?: return@synchronized
            if (generation != pendingRenderGeneration) return@synchronized
            pendingRenderGeneration = NO_PENDING_RENDER
            view = overlayView?.view
            val out = StringBuilder()
            var first = true
            for (msg in currentMessages) {
                if (!first) {
                    out.append('\n')
                }
                out.append(msg)
                first = false
            }
            text = out.toString()
        }
        val finalText = text ?: return
        view?.setText(finalText)
    }

    /**
     * Run [action] on the main thread, posting it if called from another
     * thread. Unit tests without a real [Looper] would see
     * [Looper.getMainLooper] return null and run the action synchronously
     * on the calling thread; the test suite uses Robolectric, which
     * provides a real (shadowed) main [Looper], so a call from a
     * background thread genuinely posts and can be observed as pending
     * until the test drains the shadow looper.
     *
     * @param action Action to run on the main thread
     */
    private fun postToMainThread(action: Runnable) {
        val mainLooper = Looper.getMainLooper() // Unit tests return null
        if (mainLooper != null && Thread.currentThread() != mainLooper.thread) {
            mainHandler.post(action)
        } else {
            action.run()
        }
    }

    /** Calls [OverlayView.show] and logs, but never throws, a failed result. */
    private fun showOverlay() {
        val handle = overlayView ?: return
        logFailure(handle.show(), "show")
    }

    /** Calls [OverlayView.hide] and logs, but never throws, a failed result. */
    private fun hideOverlay() {
        val handle = overlayView ?: return
        logFailure(handle.hide(), "hide")
    }

    /**
     * Logs a failed [OverlayResult] through the library [Logger] without throwing. This never
     * requests the overlay permission on the host's behalf -- a denied [OverlayResult] is simply
     * reported and the tree otherwise continues normally.
     *
     * [result] is nullable purely as a defensive measure against a mocked [OverlayView] returning
     * `null` in tests despite [OverlayView.show] and [OverlayView.hide] declaring a non-null
     * [OverlayResult]; production callers never produce `null` here.
     */
    private fun logFailure(result: OverlayResult?, action: String) {
        if (result == null || result.isSuccess) return
        val cause = result.cause
        if (cause != null) {
            Logger.w(cause, "DebugOverlayTree %s failed: %s", action, result.failure)
        } else {
            Logger.w("DebugOverlayTree %s failed: %s", action, result.failure)
        }
    }

    /** [dispose] is `@MainThread`-only on every branch, including the early uninitialized/no-op return. */
    private fun requireMainThread() {
        val main = Looper.getMainLooper()
        check(main != null && Looper.myLooper() === main) {
            "DebugOverlayTree.dispose() must be called on the main thread."
        }
    }

    private var activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks = object : SimpleActivityLifecycleCallbacks() {

        override fun onActivityStarted(activity: Activity) {
            Logger.d("onActivityStarted %s", activity)
            runningActivities!!.add(activity)
            if (registeredActivities!!.contains(activity)) {
                registeredAndRunningActivities!!.add(activity)
                showOverlay()
            }
        }

        override fun onActivityStopped(activity: Activity) {
            Logger.d("onActivityStopped %s", activity)
            if (registeredActivities!!.contains(activity) || registeredActivities!!.isEmpty()) {
                registeredAndRunningActivities!!.remove(activity)
                if (registeredAndRunningActivities!!.isEmpty()) {
                    hideOverlay()
                }
            }
            runningActivities!!.remove(activity)
        }

        override fun onActivityDestroyed(activity: Activity) {
            Logger.d("onActivityDestroyed %s", activity)
            registeredActivities?.remove(activity)
            runningActivities?.remove(activity)
            registeredAndRunningActivities?.remove(activity)
        }
    }
}
