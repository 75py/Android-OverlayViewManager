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

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Build;
import androidx.annotation.RestrictTo;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

import com.nagopy.android.overlayviewmanager.OverlayView;

import static androidx.annotation.RestrictTo.Scope.LIBRARY;
import static androidx.annotation.RestrictTo.Scope.TESTS;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;

/**
 * Inner class. Monitor the screen and calculate the height of the status bar.
 */
@RestrictTo(LIBRARY)
public class ScreenMonitor {

    ScreenMonitor() {
    }

    static ScreenMonitor INSTANCE = new ScreenMonitor();

    public static ScreenMonitor getInstance() {
        return INSTANCE;
    }

    @RestrictTo(TESTS)
    public static void setInstance(ScreenMonitor screenMonitor) {
        INSTANCE = screenMonitor;
    }

    ScreenMonitorView monitorView;
    WindowManager.LayoutParams params;
    WeakReferenceCache<OverlayView<?>> requestedViews;
    OverlayWindowManager overlayWindowManager;

    public static void init(Context context) {
        INSTANCE.initialize(context);
    }

    void initialize(Context context) {
        monitorView = new ScreenMonitorView(context);
        params = new WindowManager.LayoutParams();
        params.width = 1;
        params.height = MATCH_PARENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            params.type = TYPE_APPLICATION_OVERLAY;
        } else {
            params.type = TYPE_SYSTEM_OVERLAY;
        }
        params.flags = FLAG_NOT_TOUCHABLE;
        params.flags |= FLAG_NOT_TOUCH_MODAL;
        params.flags |= FLAG_NOT_FOCUSABLE;
        params.format = PixelFormat.TRANSLUCENT;
        requestedViews = new WeakReferenceCache<>();

        overlayWindowManager = OverlayWindowManager.getApplicationInstance();
    }

    public synchronized void request(OverlayView overlayView) {
        if (requestedViews.isEmpty()) {
            // First request
            Logger.d("Start monitoring");
            overlayWindowManager.show(monitorView, params);
        }
        requestedViews.add(overlayView);
    }

    public synchronized void cancel(OverlayView overlayView) {
        if (!requestedViews.isEmpty()) {
            requestedViews.remove(overlayView);
            if (requestedViews.isEmpty()) {
                // Stop
                Logger.d("Stop monitoring");
                overlayWindowManager.hide(monitorView);
            }
        }
    }

    /**
     * For unit test. If ScreenMonitorView is attached, it cannot find the overlay view by Espresso.
     */
    @RestrictTo(TESTS)
    public synchronized void cancelForce() {
        overlayWindowManager.hide(monitorView);
    }

    /**
     * Get current height of the status bar.
     *
     * @return Height of the status bar
     */
    public int getStatusBarHeight() {
        return monitorView.statusBarHeight;
    }

    static class ScreenMonitorView extends View implements ViewTreeObserver.OnGlobalLayoutListener {

        int statusBarHeight;

        public ScreenMonitorView(Context context) {
            super(context);
            statusBarHeight = 0;
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            getViewTreeObserver().addOnGlobalLayoutListener(this);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            getViewTreeObserver().removeOnGlobalLayoutListener(this);
        }

        @Override
        public void onGlobalLayout() {
            Rect outRect = new Rect();
            getWindowVisibleDisplayFrame(outRect);

            statusBarHeight = outRect.top;
            Logger.d("Update statusBarHeight %d", statusBarHeight);
        }

    }

}
