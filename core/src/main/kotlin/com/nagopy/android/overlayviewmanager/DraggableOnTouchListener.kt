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

package com.nagopy.android.overlayviewmanager

import android.annotation.SuppressLint
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import androidx.annotation.VisibleForTesting
import com.nagopy.android.overlayviewmanager.internal.Logger
import com.nagopy.android.overlayviewmanager.internal.OverlayGeometry
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowFrame

/**
 * Temporary 2.x migration bridge that installs the library's drag gesture on a managed view.
 *
 * `x`/`y` are always converted from screen space into the target window's layout space using
 * [OverlayGeometry] and [OverlayWindowFrame] -- see [OverlayGeometry] for the coordinate model.
 * [onTouchDown] establishes the window-relative drag origin by converting the view's current
 * screen location through the window's visible frame; [onTouchMove] then accumulates raw
 * (screen-space) touch deltas onto that origin. A delta between two screen-space points equals
 * the same delta between the corresponding window-space points (translation is frame-invariant),
 * so [onTouchMove] does not need to re-query the frame on every event.
 */
public class DraggableOnTouchListener<T : View>(
    internal val overlayView: OverlayView<T>,
) : View.OnTouchListener {

    internal var backupAlpha: Float = overlayView.spec.alpha
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var posX = 0f
    private var posY = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, ev: MotionEvent): Boolean {
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                Logger.d("ACTION_DOWN")
                onTouchDown(ev)
            }
            MotionEvent.ACTION_MOVE -> {
                Logger.d("ACTION_MOVE")
                onTouchMove(ev)
            }
            MotionEvent.ACTION_UP -> {
                Logger.d("ACTION_UP")
                overlayView.setAlpha(backupAlpha).update()
            }
            MotionEvent.ACTION_CANCEL -> {
                Logger.d("ACTION_CANCEL")
                overlayView.setAlpha(backupAlpha).update()
            }
        }
        return false
    }

    @VisibleForTesting
    internal fun onTouchMove(ev: MotionEvent) {
        val x = ev.rawX
        val y = ev.rawY

        // Calculate the distance moved
        val dx = x - lastTouchX
        val dy = y - lastTouchY
        Logger.d("x:%f, y:%f, dx:%f, dy:%f", x, y, dx, dy)

        posX += dx
        posY += dy
        Logger.d("posX:%f, posY:%f", posX, posY)

        // Update layout position
        overlayView
            .setX(posX.toInt())
            .setY(posY.toInt())
            .update()

        // Remember this touch position for the next move event
        lastTouchX = x
        lastTouchY = y
    }

    @VisibleForTesting
    internal fun onTouchDown(ev: MotionEvent) {
        val x = ev.rawX
        val y = ev.rawY

        // Remember where we started (for dragging)
        lastTouchX = x
        lastTouchY = y
        Logger.d("lastTouchX:%f, lastTouchY:%f", lastTouchX, lastTouchY)

        backupAlpha = overlayView.spec.alpha

        // Convert the view's current screen location into the target window's layout space
        // instead of assuming the display origin or a fixed status-bar inset.
        val view = overlayView.view
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val frame = OverlayWindowFrame.of(view)
        val windowX = OverlayGeometry.screenToWindowX(location[0], frame)
        val windowY = OverlayGeometry.screenToWindowY(location[1], frame)

        overlayView.update(
            overlayView.spec.toBuilder()
                .setAlpha(backupAlpha * 0.6f)
                .setVerticalMargin(0f)
                .setHorizontalMargin(0f)
                .setX(windowX)
                .setY(windowY)
                .setGravity(Gravity.TOP or Gravity.LEFT)
                .build(),
        )

        posX = windowX.toFloat()
        posY = windowY.toFloat()
        Logger.d("posX:%f, posY:%f", posX, posY)
    }
}
