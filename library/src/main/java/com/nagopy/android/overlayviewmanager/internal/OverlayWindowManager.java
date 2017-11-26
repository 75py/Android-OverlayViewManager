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

package com.nagopy.android.overlayviewmanager.internal;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.view.View;
import android.view.WindowManager;

import java.util.WeakHashMap;

import static android.support.annotation.RestrictTo.Scope.LIBRARY;
import static android.support.annotation.RestrictTo.Scope.TESTS;

@RestrictTo(LIBRARY)
public class OverlayWindowManager {

    static OverlayWindowManager applicationInstance = new OverlayWindowManager();
    static Handler handler = new Handler(Looper.getMainLooper());
    static WeakHashMap<Activity, OverlayWindowManager> activityInstances = new WeakHashMap<>();

    public static OverlayWindowManager getApplicationInstance() {
        return applicationInstance;
    }

    @RestrictTo(TESTS)
    public static void setApplicationInstance(OverlayWindowManager applicationInstance) {
        OverlayWindowManager.applicationInstance = applicationInstance;
    }

    public static void initApplicationInstance(WindowManager windowManager) {
        applicationInstance.windowManager = windowManager;
    }

    public static OverlayWindowManager getActivityInstance(Activity activity) {
        OverlayWindowManager activityInstance = activityInstances.get(activity);
        if (activityInstance == null) {
            activityInstance = new OverlayWindowManager();
            activityInstance.windowManager = activity.getWindowManager();
            activityInstances.put(activity, activityInstance);
        }
        return activityInstance;
    }

    @RestrictTo(TESTS)
    public static void setActivityInstance(Activity activity, OverlayWindowManager activityInstance) {
        activityInstances.put(activity, activityInstance);
    }


    WindowManager windowManager;

    OverlayWindowManager() {
    }

    public void show(final View view, final WindowManager.LayoutParams params) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Logger.v("Call WindowManager#addView");
                    windowManager.addView(view, params);
                } catch (Exception e) {
                    Logger.v(e, "Fail: WindowManager#addView");
                }
            }
        });
    }

    public void update(final View view, final WindowManager.LayoutParams params) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Logger.v("Call WindowManager#updateViewLayout");
                    windowManager.updateViewLayout(view, params);
                } catch (Exception e) {
                    Logger.v(e, "Fail: WindowManager#removeView");
                }
            }
        });
    }

    public void hide(final View view) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    Logger.v("Call WindowManager#removeView");
                    windowManager.removeView(view);
                } catch (Exception e) {
                    Logger.v(e, "Fail: WindowManager#removeView");
                }
            }
        });
    }

    @VisibleForTesting
    void runOnUiThread(Runnable action) {
        Looper mainLooper = Looper.getMainLooper(); // Unit tests return null
        if (mainLooper != null && Thread.currentThread().equals(mainLooper.getThread())) {
            handler.post(action);
        } else {
            action.run();
        }
    }
}
