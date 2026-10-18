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
import android.view.ViewConfiguration
import androidx.annotation.VisibleForTesting
import com.nagopy.android.overlayviewmanager.internal.Logger
import com.nagopy.android.overlayviewmanager.internal.OverlayGeometry
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowFrame
import kotlin.math.abs

/**
 * Library-owned drag gesture for [OverlayTouchMode.DRAGGABLE]. It is not part of the 3.0 public
 * API: Kotlin consumers cannot see this `internal` declaration, and no public member exposes it.
 *
 * `x`/`y` are always converted from screen space into the target window's layout space using
 * [OverlayGeometry] and [OverlayWindowFrame] -- see [OverlayGeometry] for the coordinate model.
 * A drag is not applied to the spec until [ViewConfiguration.getScaledTouchSlop] is exceeded.
 *
 * ## Consumption policy
 *
 * [onTouch] returns `true` for `ACTION_DOWN` only when the managed [View] is neither
 * [View.isClickable] nor [View.isLongClickable]: there is no click to preserve, and consuming
 * `DOWN` guarantees this listener keeps seeing every later event of the gesture regardless of
 * what a parent would otherwise decide from an unconsumed `DOWN`. When the view is clickable or
 * long-clickable, `DOWN` returns `false` so the view's own [View.onTouchEvent] tracks the press
 * and can still perform an ordinary click. Once slop is exceeded, the gesture becomes a drag
 * unconditionally: every later event of it is consumed, and [View.cancelLongPress] plus clearing
 * [View.isPressed] cancel a pending click/long-press on a clickable view so `ACTION_UP` cannot
 * fire one after a drag.
 *
 * ## Failure recovery
 *
 * The drag only advances its internal "last applied" position and raw touch reference after the
 * corresponding [OverlayView.update] call actually succeeds. A rejected backend update therefore
 * neither changes the effective spec (guaranteed by [OverlayView] itself) nor desyncs the
 * listener: the next move recomputes the full accumulated delta since the last success against
 * the same base position, so the drag continues from the effective spec's last applied position
 * once updates start succeeding again, rather than jumping by whatever was attempted meanwhile.
 *
 * A second pointer touching down never changes which pointer drives the drag; if the active
 * pointer itself lifts while others remain, tracking rebases onto a remaining pointer's current
 * position instead of its stale prior position, so the overlay does not jump.
 */
internal class DraggableOnTouchListener<T : View>(
    private val overlayView: OverlayView<T>,
) : View.OnTouchListener {

    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var isDragging = false
    private var dragApplied = false
    private var consumesFromDown = false
    private var downRawX = 0f
    private var downRawY = 0f
    private var lastRawX = 0f
    private var lastRawY = 0f
    private var originWindowX = 0
    private var originWindowY = 0
    private var posX = 0f
    private var posY = 0f
    internal var backupAlpha: Float = overlayView.spec.alpha

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(view: View, ev: MotionEvent): Boolean = when (ev.actionMasked) {
        MotionEvent.ACTION_DOWN -> {
            Logger.d("ACTION_DOWN")
            onActionDown(view, ev)
        }
        MotionEvent.ACTION_POINTER_DOWN -> consumed()
        MotionEvent.ACTION_MOVE -> onActionMove(view, ev)
        MotionEvent.ACTION_POINTER_UP -> {
            onActionPointerUp(ev)
            consumed()
        }
        MotionEvent.ACTION_UP -> {
            Logger.d("ACTION_UP")
            onActionEnd()
        }
        MotionEvent.ACTION_CANCEL -> {
            Logger.d("ACTION_CANCEL")
            onActionEnd()
        }
        else -> consumed()
    }

    private fun consumed(): Boolean = consumesFromDown || isDragging

    private fun onActionDown(view: View, ev: MotionEvent): Boolean {
        activePointerId = ev.getPointerId(0)
        isDragging = false
        dragApplied = false
        downRawX = ev.rawX
        downRawY = ev.rawY
        lastRawX = downRawX
        lastRawY = downRawY
        backupAlpha = overlayView.spec.alpha

        // Convert the view's current screen location into the target window's layout space
        // instead of assuming the display origin or a fixed status-bar inset.
        val location = IntArray(2)
        view.getLocationOnScreen(location)
        val frame = OverlayWindowFrame.of(view)
        originWindowX = OverlayGeometry.screenToWindowX(location[0], frame.left)
        originWindowY = OverlayGeometry.screenToWindowY(location[1], frame.top)
        posX = originWindowX.toFloat()
        posY = originWindowY.toFloat()

        consumesFromDown = !view.isClickable && !view.isLongClickable
        return consumesFromDown
    }

    @VisibleForTesting
    internal fun onActionMove(view: View, ev: MotionEvent): Boolean {
        val pointerIndex = ev.findPointerIndex(activePointerId)
        if (pointerIndex == -1) return consumed()
        val (rawX, rawY) = rawCoordinates(ev, pointerIndex)

        if (!isDragging) {
            val dx = rawX - downRawX
            val dy = rawY - downRawY
            val slop = ViewConfiguration.get(view.context).scaledTouchSlop
            if (abs(dx) < slop && abs(dy) < slop) return consumesFromDown
            isDragging = true
            cancelPendingClick(view)
        }

        // Only ever advance from the last successfully applied position: if the backend rejects
        // this attempt, the next move recomputes the full delta since that last success instead
        // of compounding an unapplied displacement.
        val candidateX = posX + (rawX - lastRawX)
        val candidateY = posY + (rawY - lastRawY)
        val result = if (!dragApplied) {
            overlayView.update(
                overlayView.spec.toBuilder()
                    .setAlpha(backupAlpha * 0.6f)
                    .setVerticalMargin(0f)
                    .setHorizontalMargin(0f)
                    .setX(candidateX.toInt())
                    .setY(candidateY.toInt())
                    .setGravity(Gravity.TOP or Gravity.LEFT)
                    .build(),
            )
        } else {
            overlayView.update(overlayView.spec.copy(x = candidateX.toInt(), y = candidateY.toInt()))
        }
        Logger.d("candidateX:%f, candidateY:%f, applied:%b", candidateX, candidateY, result.isSuccess)
        if (result.isSuccess) {
            posX = candidateX
            posY = candidateY
            lastRawX = rawX
            lastRawY = rawY
            dragApplied = true
        }
        return true
    }

    private fun cancelPendingClick(view: View) {
        view.cancelLongPress()
        view.isPressed = false
    }

    private fun onActionPointerUp(ev: MotionEvent) {
        val liftedIndex = ev.actionIndex
        if (ev.getPointerId(liftedIndex) != activePointerId) return
        // The pointer driving the gesture just lifted while another remains: rebase onto that
        // pointer's current position so the next move delta does not jump.
        val newIndex = if (liftedIndex == 0) 1 else 0
        if (newIndex >= ev.pointerCount) {
            activePointerId = MotionEvent.INVALID_POINTER_ID
            return
        }
        activePointerId = ev.getPointerId(newIndex)
        val (rawX, rawY) = rawCoordinates(ev, newIndex)
        lastRawX = rawX
        lastRawY = rawY
        if (!isDragging) {
            // The slop origin must also move to the new pointer, or a later stationary move from
            // it would be measured against the old pointer's down point and spuriously exceed slop.
            downRawX = rawX
            downRawY = rawY
        }
    }

    private fun onActionEnd(): Boolean {
        val wasDragging = isDragging
        if (wasDragging) {
            overlayView.update(overlayView.spec.copy(alpha = backupAlpha))
        }
        val result = consumed()
        isDragging = false
        dragApplied = false
        activePointerId = MotionEvent.INVALID_POINTER_ID
        consumesFromDown = false
        return result
    }

    /**
     * The screen-space (raw) coordinates of [pointerIndex] within [ev]. [MotionEvent.getRawX]
     * without an index argument only ever reports pointer index 0 (pre-API 29 has no indexed
     * overload), so an arbitrary active pointer's raw position is derived from the constant
     * view-to-screen offset that [MotionEvent.getRawX]/[MotionEvent.getX] expose for pointer 0.
     */
    private fun rawCoordinates(ev: MotionEvent, pointerIndex: Int): Pair<Float, Float> {
        val offsetX = ev.rawX - ev.getX(0)
        val offsetY = ev.rawY - ev.getY(0)
        return (ev.getX(pointerIndex) + offsetX) to (ev.getY(pointerIndex) + offsetY)
    }
}
