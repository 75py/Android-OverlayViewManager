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
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;

import com.nagopy.android.overlayviewmanager.OverlayView;
import com.nagopy.android.overlayviewmanager.internal.Logger;
import com.nagopy.android.overlayviewmanager.internal.SimpleActivityLifecycleCallbacks;
import com.nagopy.android.overlayviewmanager.internal.WeakReferenceCache;

import java.util.ArrayList;

import timber.log.Timber;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;

/**
 * [Option] Implementation of {@link Timber.Tree}.
 * Usage:
 * <pre><code>
 * public class SampleApplication extends Application {
 *
 *     &#64;Override
 *     public void onCreate() {
 *         super.onCreate();
 *
 *         OverlayViewManager.init(this);
 *
 *         Timber.plant(new Timber.DebugTree());
 *         Timber.plant(DebugOverlayTree.init(this));
 *     }
 *
 * }
 * </code></pre>
 */
public class DebugOverlayTree extends Timber.DebugTree {

    ArrayList<String> messages;
    OverlayView<TextView> overlayView;
    WeakReferenceCache<Activity> registeredActivities;
    int threshold;
    int maxLines;

    private static final DebugOverlayTree INSTANCE = new DebugOverlayTree();

    /**
     * Initialize and return the {@link Timber.Tree} implementation.
     * Usage: <code>Timber.plant(DebugOverlayTree.init(this /&#42; Application &#42;/));</code>
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
            throw new IllegalArgumentException("DebugOverlayTree is not initialized. Please call init(Context).");
        }
        return INSTANCE;
    }

    DebugOverlayTree() {
        overlayView = null;
    }

    /**
     * Inner method. Initialize members.
     *
     * @param application Application
     */
    void initialize(Application application) {
        messages = new ArrayList<>();
        threshold = Log.DEBUG;
        maxLines = 5;
        overlayView = OverlayView.create(new TextView(application))
                .setAlpha(0.4f)
                .setGravity(Gravity.BOTTOM)
                .setWidth(MATCH_PARENT);
        overlayView.getView().setTextColor(Color.WHITE);
        overlayView.getView().setBackgroundColor(Color.BLACK);

        registeredActivities = new WeakReferenceCache<>();
        application.registerActivityLifecycleCallbacks(new SimpleActivityLifecycleCallbacks() {
            int startedCount = 0;

            @Override
            public void onActivityStarted(Activity activity) {
                Logger.d("onActivityStarted %s", activity);
                if (registeredActivities.contains(activity)) {
                    startedCount++;
                    overlayView.show();
                }
            }

            @Override
            public void onActivityStopped(Activity activity) {
                Logger.d("onActivityStopped %s", activity);
                if (registeredActivities.contains(activity) || registeredActivities.isEmpty()) {
                    startedCount--;
                    if (startedCount <= 0) {
                        startedCount = 0;
                        overlayView.hide();
                    }
                }
            }

            @Override
            public void onActivityDestroyed(Activity activity) {
                Logger.d("onActivityDestroyed %s", activity);
                registeredActivities.remove(activity);
            }
        });
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
     * Set max line number.
     *
     * @param maxLines Max line number
     */
    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }

    /**
     * Register the activity. If called, the debug view is displayed while the activity is visible.
     *
     * @param activity Activity
     */
    public void register(Activity activity) {
        registeredActivities.add(activity);
    }

    @Override
    protected void log(int priority, String tag, String message, Throwable t) {
        if (priority < threshold) {
            return;
        }

        messages.add(tag + "\n  " + message);
        while (messages.size() > maxLines) {
            messages.remove(0);
        }

        StringBuilder out = new StringBuilder();
        for (String msg : messages) {
            out.append(msg);
            out.append('\n');
        }
        out.setLength(out.length() - 1);
        overlayView.getView().setText(out.toString());
    }
}
