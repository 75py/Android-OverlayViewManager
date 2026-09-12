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

package com.nagopy.android.overlayviewmanager.opt.timber;


import android.app.Activity;
import android.app.Application;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.VisibleForTesting;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;

import com.nagopy.android.overlayviewmanager.OverlayView;
import com.nagopy.android.overlayviewmanager.OverlayViewManager;
import com.nagopy.android.overlayviewmanager.internal.Logger;
import com.nagopy.android.overlayviewmanager.internal.SimpleActivityLifecycleCallbacks;
import com.nagopy.android.overlayviewmanager.internal.WeakReferenceCache;

import java.util.ArrayDeque;
import java.util.concurrent.atomic.AtomicBoolean;

import timber.log.Timber;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * [Option] Implementation of {@link Timber.Tree}.
 */
public class DebugOverlayTree extends Timber.DebugTree {

    @VisibleForTesting
    static final int DEFAULT_MAX_LINES = 5;

    /**
     * Guards {@link #messages} and {@link #maxLines} so that {@link #log}
     * (called from arbitrary threads) and {@link #setMaxLines} never observe
     * or produce a torn buffer.
     */
    private final Object bufferLock = new Object();

    /**
     * Set once a render {@link Runnable} has been posted to the main thread
     * and not yet executed, so that bursts of {@link #log} calls coalesce
     * into a single pending UI update instead of posting once per log line.
     */
    @VisibleForTesting
    final AtomicBoolean renderPending = new AtomicBoolean(false);

    /**
     * Read from the logging thread by {@link #postToMainThread}. Safe without
     * {@code volatile}/locking because it is assigned only once, in this
     * field initializer, which runs while {@link #INSTANCE} is created by the
     * class's static initializer; the JLS guarantees that write is visible to
     * every thread that subsequently observes an initialized
     * {@code DebugOverlayTree} class (JLS 12.4.2). It must not be reassigned
     * outside of tests, or this safe-publication argument no longer holds.
     */
    @VisibleForTesting
    Handler mainHandler = new Handler(Looper.getMainLooper());

    @VisibleForTesting
    ArrayDeque<String> messages;
    /**
     * Never read from the logging thread: only {@link #render()} (posted to
     * and always run on the main thread) and the main-thread-only
     * registration/lifecycle methods touch it, so it needs no additional
     * synchronization for the thread-safety gap this class addresses.
     */
    @VisibleForTesting
    OverlayView<TextView> overlayView;
    @VisibleForTesting
    WeakReferenceCache<Activity> registeredActivities;
    @VisibleForTesting
    WeakReferenceCache<Activity> runningActivities;
    @VisibleForTesting
    WeakReferenceCache<Activity> registeredAndRunningActivities;
    /**
     * Minimum log priority accepted by {@link #log}. {@link #setThreshold} is
     * typically called from the main thread while {@link #log} runs on
     * whatever thread Timber is logging from, so there is no lock or other
     * happens-before edge naturally connecting the two. This field is
     * {@code volatile} rather than guarded by {@link #bufferLock} so the
     * threshold check in {@link #log} stays lock-free on its fast path
     * (the common case where a line is filtered out before the buffer is
     * ever touched).
     */
    @VisibleForTesting
    volatile int threshold;
    @VisibleForTesting
    int maxLines;

    @VisibleForTesting
    static DebugOverlayTree INSTANCE = new DebugOverlayTree();

    /**
     * Initialize and return the {@link Timber.Tree} implementation.
     * Usage: <code>Timber.plant(DebugOverlayTree.initApplicationInstance(this /&#42; Application &#42;/));</code>
     *
     * @param application Your {@link Application} instance
     * @return The {@link Timber.Tree} implementation
     */
    public static DebugOverlayTree init(Application application) {
        INSTANCE.initialize(application);
        return INSTANCE;
    }

    /**
     * Return the {@link DebugOverlayTree} instance. The instance is singleton.
     *
     * @return instance
     */
    public static DebugOverlayTree getInstance() {
        if (INSTANCE.overlayView == null) {
            throw new IllegalStateException("DebugOverlayTree is not initialized. Please call initApplicationInstance(Context).");
        }
        return INSTANCE;
    }

    @VisibleForTesting
    DebugOverlayTree() {
        overlayView = null;
    }

    /**
     * Inner method. Initialize members.
     *
     * @param application Application
     */
    void initialize(Application application) {
        messages = new ArrayDeque<>();
        threshold = Log.DEBUG;
        maxLines = DEFAULT_MAX_LINES;
        overlayView = OverlayViewManager.getInstance().newOverlayView(new TextView(application))
                .setAlpha(0.4f)
                .setGravity(Gravity.BOTTOM)
                .setWidth(MATCH_PARENT);
        overlayView.getView().setTextColor(Color.WHITE);
        overlayView.getView().setBackgroundColor(Color.BLACK);

        registeredActivities = new WeakReferenceCache<>();
        runningActivities = new WeakReferenceCache<>();
        registeredAndRunningActivities = new WeakReferenceCache<>();
        application.registerActivityLifecycleCallbacks(activityLifecycleCallbacks);
    }

    /**
     * Set minimum log level.
     *
     * @param threshold Log level
     */
    public void setThreshold(int threshold) {
        this.threshold = threshold;
    }

    /**
     * Set the maximum number of retained log lines shown in the overlay.
     * <p>
     * Shrinking below the current retained line count trims the oldest lines
     * immediately and re-renders the overlay; growing takes effect for
     * subsequent log lines without touching the current buffer.
     *
     * @param maxLines Max line number, must be 1 or greater
     * @throws IllegalArgumentException if {@code maxLines} is less than 1
     */
    public void setMaxLines(int maxLines) {
        if (maxLines < 1) {
            throw new IllegalArgumentException("maxLines must be >= 1, but was " + maxLines);
        }
        boolean trimmed;
        synchronized (bufferLock) {
            this.maxLines = maxLines;
            trimmed = trimLocked();
        }
        if (trimmed) {
            scheduleRender();
        }
    }

    /**
     * Register the activity. If called, the debug view is displayed while the activity is visible.
     *
     * @param activity Activity
     */
    public void register(Activity activity) {
        registeredActivities.add(activity);
        if (runningActivities.contains(activity)) {
            registeredAndRunningActivities.add(activity);
            overlayView.show();
        }
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        if (priority < threshold) {
            return;
        }

        synchronized (bufferLock) {
            messages.addLast(tag + ": " + message);
            trimLocked();
        }
        scheduleRender();
    }

    /**
     * Drop the oldest lines until {@link #messages} fits within {@link #maxLines}.
     * Callers must hold {@link #bufferLock}.
     *
     * @return true if at least one line was dropped
     */
    private boolean trimLocked() {
        boolean removed = false;
        while (messages.size() > maxLines) {
            messages.removeFirst();
            removed = true;
        }
        return removed;
    }

    /**
     * Ensure exactly one render {@link Runnable} is pending on the main
     * thread. Concurrent callers coalesce onto that single pending render
     * instead of each posting their own.
     */
    private void scheduleRender() {
        if (renderPending.compareAndSet(false, true)) {
            postToMainThread(renderRunnable);
        }
    }

    @VisibleForTesting
    final Runnable renderRunnable = new Runnable() {
        @Override
        public void run() {
            renderPending.set(false);
            render();
        }
    };

    /**
     * Build the text for the current buffer and apply it to the TextView.
     * Only ever invoked on the main thread via {@link #renderRunnable}.
     */
    private void render() {
        String text;
        synchronized (bufferLock) {
            StringBuilder out = new StringBuilder();
            boolean first = true;
            for (String msg : messages) {
                if (!first) {
                    out.append('\n');
                }
                out.append(msg);
                first = false;
            }
            text = out.toString();
        }
        overlayView.getView().setText(text);
    }

    /**
     * Run {@code action} on the main thread, posting it if called from
     * another thread. Unit tests run without a real {@link Looper}, so
     * {@link Looper#getMainLooper()} returns null and the action runs
     * synchronously on the calling thread.
     *
     * @param action Action to run on the main thread
     */
    @VisibleForTesting
    void postToMainThread(Runnable action) {
        Looper mainLooper = Looper.getMainLooper(); // Unit tests return null
        if (mainLooper != null && !Thread.currentThread().equals(mainLooper.getThread())) {
            mainHandler.post(action);
        } else {
            action.run();
        }
    }

    @VisibleForTesting
    Application.ActivityLifecycleCallbacks activityLifecycleCallbacks = new SimpleActivityLifecycleCallbacks() {

        @Override
        public void onActivityStarted(Activity activity) {
            Logger.d("onActivityStarted %s", activity);
            runningActivities.add(activity);
            if (registeredActivities.contains(activity)) {
                registeredAndRunningActivities.add(activity);
                overlayView.show();
            }
        }

        @Override
        public void onActivityStopped(Activity activity) {
            Logger.d("onActivityStopped %s", activity);
            if (registeredActivities.contains(activity) || registeredActivities.isEmpty()) {
                registeredAndRunningActivities.remove(activity);
                if (registeredAndRunningActivities.isEmpty()) {
                    overlayView.hide();
                }
            }
            runningActivities.remove(activity);
        }

        @Override
        public void onActivityDestroyed(Activity activity) {
            Logger.d("onActivityDestroyed %s", activity);
            registeredActivities.remove(activity);
        }
    };
}
