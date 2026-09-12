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
import androidx.annotation.VisibleForTesting
import com.nagopy.android.overlayviewmanager.OverlayView
import com.nagopy.android.overlayviewmanager.OverlayViewManager
import com.nagopy.android.overlayviewmanager.internal.Logger
import com.nagopy.android.overlayviewmanager.internal.SimpleActivityLifecycleCallbacks
import com.nagopy.android.overlayviewmanager.internal.WeakReferenceCache
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean
import timber.log.Timber

/**
 * [Option] Implementation of [Timber.Tree].
 */
open class DebugOverlayTree
@VisibleForTesting
constructor() : Timber.DebugTree() {

    /**
     * Guards [messages] and [maxLines] so that [log] (called from arbitrary
     * threads) and [setMaxLines] never observe or produce a torn buffer.
     */
    private val bufferLock = Any()

    /**
     * Set once a render [Runnable] has been posted to the main thread and
     * not yet executed, so that bursts of [log] calls coalesce into a
     * single pending UI update instead of posting once per log line.
     */
    @VisibleForTesting
    @JvmField
    val renderPending: AtomicBoolean = AtomicBoolean(false)

    /**
     * Read from the logging thread by [postToMainThread]. Safe without
     * `volatile`/locking because it is assigned only once, in this field
     * initializer, which runs while [INSTANCE] is created by the class's
     * static initializer; the JLS guarantees that write is visible to
     * every thread that subsequently observes an initialized
     * [DebugOverlayTree] class (JLS 12.4.2). It must not be reassigned
     * outside of tests, or this safe-publication argument no longer holds.
     */
    @VisibleForTesting
    @JvmField
    var mainHandler: Handler = Handler(Looper.getMainLooper())

    @VisibleForTesting
    @JvmField
    var messages: ArrayDeque<String>? = null

    /**
     * Touched only by [initialize], [register], [render], and the
     * main-thread-only [Application.ActivityLifecycleCallbacks] methods
     * registered as [activityLifecycleCallbacks] (`onActivityStarted` /
     * `onActivityStopped`). [render] runs on the main thread only because
     * it is always invoked through [postToMainThread]; the other methods
     * are Android main-thread entry points. [log], which runs on whatever
     * thread Timber is logging from, never reads or writes this field, so
     * it needs no additional synchronization for the thread-safety gap
     * this class addresses.
     */
    @VisibleForTesting
    @JvmField
    var overlayView: OverlayView<TextView>? = null

    @VisibleForTesting
    @JvmField
    var registeredActivities: WeakReferenceCache<Activity>? = null

    @VisibleForTesting
    @JvmField
    var runningActivities: WeakReferenceCache<Activity>? = null

    @VisibleForTesting
    @JvmField
    var registeredAndRunningActivities: WeakReferenceCache<Activity>? = null

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
    @VisibleForTesting
    @JvmField
    var threshold: Int = 0

    @VisibleForTesting
    @JvmField
    var maxLines: Int = 0

    companion object {

        @VisibleForTesting
        const val DEFAULT_MAX_LINES: Int = 5

        @VisibleForTesting
        @JvmField
        var INSTANCE: DebugOverlayTree = DebugOverlayTree()

        /**
         * Initialize and return the [Timber.Tree] implementation.
         * Usage: `Timber.plant(DebugOverlayTree.initApplicationInstance(this /* Application */));`
         *
         * @param application Your [Application] instance
         * @return The [Timber.Tree] implementation
         */
        @JvmStatic
        fun init(application: Application): DebugOverlayTree {
            INSTANCE.initialize(application)
            return INSTANCE
        }

        /**
         * Return the [DebugOverlayTree] instance. The instance is singleton.
         *
         * @return instance
         */
        @JvmStatic
        fun getInstance(): DebugOverlayTree {
            if (INSTANCE.overlayView == null) {
                throw IllegalStateException(
                    "DebugOverlayTree is not initialized. Please call initApplicationInstance(Context).",
                )
            }
            return INSTANCE
        }
    }

    init {
        overlayView = null
    }

    /**
     * Inner method. Initialize members.
     *
     * @param application Application
     */
    @VisibleForTesting
    open fun initialize(application: Application) {
        messages = ArrayDeque()
        threshold = Log.DEBUG
        maxLines = DEFAULT_MAX_LINES
        overlayView = OverlayViewManager.getInstance().newOverlayView(TextView(application))
            .setAlpha(0.4f)
            .setGravity(Gravity.BOTTOM)
            .setWidth(MATCH_PARENT)
        overlayView!!.getView().setTextColor(Color.WHITE)
        overlayView!!.getView().setBackgroundColor(Color.BLACK)

        registeredActivities = WeakReferenceCache()
        runningActivities = WeakReferenceCache()
        registeredAndRunningActivities = WeakReferenceCache()
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks)
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
        val trimmed: Boolean
        synchronized(bufferLock) {
            this.maxLines = maxLines
            trimmed = trimLocked()
        }
        if (trimmed) {
            scheduleRender()
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
            overlayView!!.show()
        }
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        if (priority < threshold) {
            return
        }

        synchronized(bufferLock) {
            messages!!.addLast("$tag: $message")
            trimLocked()
        }
        scheduleRender()
    }

    /**
     * Drop the oldest lines until [messages] fits within [maxLines].
     * Callers must hold [bufferLock].
     *
     * @return true if at least one line was dropped
     */
    private fun trimLocked(): Boolean {
        var removed = false
        while (messages!!.size > maxLines) {
            messages!!.removeFirst()
            removed = true
        }
        return removed
    }

    /**
     * Ensure exactly one render [Runnable] is pending on the main thread.
     * Concurrent callers coalesce onto that single pending render instead
     * of each posting their own.
     */
    private fun scheduleRender() {
        if (renderPending.compareAndSet(false, true)) {
            postToMainThread(renderRunnable)
        }
    }

    @VisibleForTesting
    @JvmField
    val renderRunnable: Runnable = Runnable {
        renderPending.set(false)
        render()
    }

    /**
     * Build the text for the current buffer and apply it to the TextView.
     * Only ever invoked on the main thread via [renderRunnable].
     */
    private fun render() {
        val text: String
        synchronized(bufferLock) {
            val out = StringBuilder()
            var first = true
            for (msg in messages!!) {
                if (!first) {
                    out.append('\n')
                }
                out.append(msg)
                first = false
            }
            text = out.toString()
        }
        overlayView!!.getView().setText(text)
    }

    /**
     * Run [action] on the main thread, posting it if called from another
     * thread. Unit tests run without a real [Looper], so
     * [Looper.getMainLooper] returns null and the action runs
     * synchronously on the calling thread.
     *
     * @param action Action to run on the main thread
     */
    @VisibleForTesting
    open fun postToMainThread(action: Runnable) {
        val mainLooper = Looper.getMainLooper() // Unit tests return null
        if (mainLooper != null && Thread.currentThread() != mainLooper.thread) {
            mainHandler.post(action)
        } else {
            action.run()
        }
    }

    @VisibleForTesting
    @JvmField
    val activityLifecycleCallbacks: Application.ActivityLifecycleCallbacks = object : SimpleActivityLifecycleCallbacks() {

        override fun onActivityStarted(activity: Activity) {
            Logger.d("onActivityStarted %s", activity)
            runningActivities!!.add(activity)
            if (registeredActivities!!.contains(activity)) {
                registeredAndRunningActivities!!.add(activity)
                overlayView!!.show()
            }
        }

        override fun onActivityStopped(activity: Activity) {
            Logger.d("onActivityStopped %s", activity)
            if (registeredActivities!!.contains(activity) || registeredActivities!!.isEmpty()) {
                registeredAndRunningActivities!!.remove(activity)
                if (registeredAndRunningActivities!!.isEmpty()) {
                    overlayView!!.hide()
                }
            }
            runningActivities!!.remove(activity)
        }

        override fun onActivityDestroyed(activity: Activity) {
            Logger.d("onActivityDestroyed %s", activity)
            registeredActivities!!.remove(activity)
        }
    }
}
