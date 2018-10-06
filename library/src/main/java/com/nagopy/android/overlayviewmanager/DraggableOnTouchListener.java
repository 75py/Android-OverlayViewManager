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

package com.nagopy.android.overlayviewmanager;

import android.annotation.SuppressLint;
import androidx.annotation.VisibleForTesting;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;

import com.nagopy.android.overlayviewmanager.internal.Logger;
import com.nagopy.android.overlayviewmanager.internal.ScreenMonitor;

public class DraggableOnTouchListener<T extends View> implements View.OnTouchListener {

    OverlayView<T> overlayView;
    ScreenMonitor screenMonitor;
    float backupAlpha;
    private float mLastTouchX;
    private float mLastTouchY;
    private float mPosX;
    private float mPosY;

    DraggableOnTouchListener(OverlayView<T> overlayView) {
        this.overlayView = overlayView;
        this.screenMonitor = ScreenMonitor.getInstance();
        backupAlpha = overlayView.params.alpha;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent ev) {
        final int action = ev.getActionMasked();

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                Logger.d("ACTION_DOWN");
                onTouchDown(ev);
                break;

            case MotionEvent.ACTION_MOVE:
                Logger.d("ACTION_MOVE");
                onTouchMove(ev);
                break;

            case MotionEvent.ACTION_UP:
                Logger.d("ACTION_UP");
                overlayView.setAlpha(backupAlpha).update();
                break;

            case MotionEvent.ACTION_CANCEL:
                Logger.d("ACTION_CANCEL");
                overlayView.setAlpha(backupAlpha).update();
                break;
        }

        return false;
    }

    @VisibleForTesting
    void onTouchMove(MotionEvent ev) {
        final float x = ev.getRawX();
        final float y = ev.getRawY();

        // Calculate the distance moved
        final float dx = x - mLastTouchX;
        final float dy = y - mLastTouchY;
        Logger.d("x:%f, y:%f, dx:%f, dy:%f", x, y, dx, dy);

        mPosX += dx;
        mPosY += dy;
        Logger.d("mPosX:%f, mPosY:%f", mPosX, mPosY);

        // Update layout position
        overlayView
                .setX((int) mPosX)
                .setY((int) mPosY)
                .update();


        // Remember this touch position for the next move event
        mLastTouchX = x;
        mLastTouchY = y;
    }

    @VisibleForTesting
    void onTouchDown(MotionEvent ev) {
        final float x = ev.getRawX();
        final float y = ev.getRawY();

        // Remember where we started (for dragging)
        mLastTouchX = x;
        mLastTouchY = y;
        Logger.d("mLastTouchX:%f, mLastTouchY:%f", mLastTouchX, mLastTouchY);

        backupAlpha = overlayView.params.alpha;
        overlayView.params.alpha *= 0.6;

        int statusBarHeight = screenMonitor.getStatusBarHeight();

        // Disable verticalMargin/horizontalMargin and set current position to XY.
        overlayView.params.verticalMargin = 0;
        overlayView.params.horizontalMargin = 0;
        int[] location = new int[2];
        overlayView.view.getLocationOnScreen(location);
        overlayView.params.x = location[0];
        overlayView.params.y = location[1] - statusBarHeight;
        overlayView.params.gravity = Gravity.TOP | Gravity.LEFT;
        overlayView.update();

        mPosX = location[0];
        mPosY = location[1] - statusBarHeight;
        Logger.d("mPosX:%f, mPosY:%f", mPosX, mPosY);
    }

}
