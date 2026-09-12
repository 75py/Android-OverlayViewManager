package com.nagopy.android.overlayviewmanager

import android.app.Activity
import android.graphics.Rect
import android.os.Build
import android.os.Looper
import android.view.InputDevice
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowManager
import java.time.Duration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings

/**
 * Covers the internal drag gesture: touch-slop-gated consumption, active-pointer tracking across
 * a second finger, and alpha/effective-spec restoration on UP/CANCEL and on a rejected update.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], manifest = Config.NONE)
class DraggableOnTouchListenerTest {

    @Test fun tapBelowSlopFiresClickAndLeavesSpecUnchanged() {
        val view = PositionedView(100, 240, 24)
        val overlay = OverlayView(
            view,
            OverlayScope.ACTIVITY,
            RecordingBackend(),
            OverlaySpec(touchMode = OverlayTouchMode.DRAGGABLE),
        )
        overlay.show()
        var clicks = 0
        overlay.view.setOnClickListener { clicks++ }
        val before = overlay.spec
        val slop = touchSlop(view)

        assertFalse(dispatch(view, MotionEvent.ACTION_DOWN, 110f, 260f))
        assertFalse(dispatch(view, MotionEvent.ACTION_MOVE, 110f + slop - 1, 260f))
        assertFalse(dispatch(view, MotionEvent.ACTION_UP, 110f + slop - 1, 260f))

        assertEquals(1, clicks)
        assertEquals(before, overlay.spec)
    }

    @Test fun moveBeyondSlopConsumesAndUpdatesXY() {
        val view = PositionedView(100, 240, 24)
        val backend = RecordingBackend()
        val overlay = OverlayView(
            view,
            OverlayScope.ACTIVITY,
            backend,
            OverlaySpec(alpha = .8f, touchMode = OverlayTouchMode.DRAGGABLE),
        )
        overlay.show()
        val slop = touchSlop(view)

        val afterShow = overlay.spec
        assertFalse(dispatch(view, MotionEvent.ACTION_DOWN, 110f, 260f))
        assertEquals(afterShow, overlay.spec)
        assertEquals(.8f, overlay.spec.alpha, 0f)

        assertTrue(dispatch(view, MotionEvent.ACTION_MOVE, 110f + slop + 20, 260f))
        assertEquals(100 + slop + 20, overlay.spec.x)
        assertEquals(216, overlay.spec.y)
        assertEquals(.48f, overlay.spec.alpha, .0001f)

        assertTrue(dispatch(view, MotionEvent.ACTION_MOVE, 110f + slop + 50, 290f))
        assertEquals(100 + slop + 50, overlay.spec.x)
        assertEquals(246, overlay.spec.y)

        assertTrue(dispatch(view, MotionEvent.ACTION_UP, 110f + slop + 50, 290f))
        assertEquals(.8f, overlay.spec.alpha, 0f)
        assertEquals(100 + slop + 50, overlay.spec.x)
        assertEquals(246, overlay.spec.y)
        assertEquals(3, backend.updateCalls)
    }

    @Test fun downMove_dragAcrossANonZeroWindowOriginIsWindowRelative() {
        // The fake view's screen location is (300, 500); its stubbed visible frame starts at
        // (120, 60) (e.g. a multi-window/freeform window), so the layout-space origin is (120, 60)
        // rather than the display's (0, 0). Reuses the T05b PositionedView fixture: this proves
        // the listener and OverlayView.layoutParams() correctly consume whatever frame
        // OverlayWindowFrame returns, not that the real framework reports this shape on a device
        // (see OverlayWindowFrame's KDoc).
        val view = PositionedView(screenX = 300, screenY = 500, visibleFrameLeft = 120, visibleFrameTop = 60)
        val backend = RecordingBackend()
        val overlay = OverlayView(
            view,
            OverlayScope.ACTIVITY,
            backend,
            OverlaySpec(touchMode = OverlayTouchMode.DRAGGABLE),
        )
        overlay.show()
        val slop = touchSlop(view)

        assertFalse(dispatch(view, MotionEvent.ACTION_DOWN, 310f, 520f))
        assertTrue(dispatch(view, MotionEvent.ACTION_MOVE, 310f + slop + 20, 545f))
        assertEquals(180 + slop + 20, overlay.spec.x)
        assertEquals(465, overlay.spec.y)
        // Goes through the real WindowManager.LayoutParams built by OverlayView.layoutParams(),
        // not just the immutable OverlaySpec snapshot.
        assertEquals(180 + slop + 20, backend.lastParams!!.x)
        assertEquals(465, backend.lastParams!!.y)

        assertTrue(dispatch(view, MotionEvent.ACTION_MOVE, 330f + slop + 20, 545f))
        assertEquals(200 + slop + 20, overlay.spec.x)
        assertEquals(465, overlay.spec.y)
    }

    @Test fun downMoveCancelRestoresOriginalAlphaWithoutResettingCoordinates() {
        val view = PositionedView(50, 160, 20)
        val overlay = OverlayView(
            view,
            OverlayScope.ACTIVITY,
            RecordingBackend(),
            OverlaySpec(alpha = .75f, touchMode = OverlayTouchMode.DRAGGABLE),
        )
        overlay.show()
        val slop = touchSlop(view)

        assertFalse(dispatch(view, MotionEvent.ACTION_DOWN, 10f, 30f))
        assertTrue(dispatch(view, MotionEvent.ACTION_MOVE, 10f + slop + 10, 55f))
        val draggedX = overlay.spec.x
        val draggedY = overlay.spec.y
        assertTrue(dispatch(view, MotionEvent.ACTION_CANCEL, 10f + slop + 10, 55f))
        assertEquals(draggedX, overlay.spec.x)
        assertEquals(draggedY, overlay.spec.y)
        assertEquals(.75f, overlay.spec.alpha, 0f)
    }

    @Test fun cancelBelowSlopIsANoOpAndReturnsFalse() {
        val view = PositionedView(50, 160, 20)
        val overlay = OverlayView(
            view,
            OverlayScope.ACTIVITY,
            RecordingBackend(),
            OverlaySpec(alpha = .75f, touchMode = OverlayTouchMode.DRAGGABLE),
        )
        overlay.show()
        val before = overlay.spec

        assertFalse(dispatch(view, MotionEvent.ACTION_DOWN, 10f, 30f))
        assertFalse(dispatch(view, MotionEvent.ACTION_CANCEL, 11f, 31f))
        assertEquals(before, overlay.spec)
    }

    @Test fun secondFingerPointerDownDoesNotMoveTheOverlayAndActivePointerKeepsDriving() {
        val view = PositionedView(100, 240, 24)
        val overlay = OverlayView(
            view,
            OverlayScope.ACTIVITY,
            RecordingBackend(),
            OverlaySpec(touchMode = OverlayTouchMode.DRAGGABLE),
        )
        overlay.show()
        val slop = touchSlop(view)

        assertFalse(dispatch(view, MotionEvent.ACTION_DOWN, 110f, 260f))
        assertTrue(dispatch(view, MotionEvent.ACTION_MOVE, 110f + slop + 20, 260f))
        val afterFirstMoveX = overlay.spec.x
        val afterFirstMoveY = overlay.spec.y

        // A second finger touches down far away; the drag must not jump to it.
        val pointerDown = obtainMultiTouch(
            MotionEvent.ACTION_POINTER_DOWN,
            listOf(Triple(0, 110f + slop + 20, 260f), Triple(1, 900f, 900f)),
            actionIndex = 1,
        )
        assertTrue(view.dispatchTouchEvent(pointerDown))
        pointerDown.recycle()
        assertEquals(afterFirstMoveX, overlay.spec.x)
        assertEquals(afterFirstMoveY, overlay.spec.y)

        // Pointer 0 (still active) keeps moving; pointer 1 moving elsewhere must not affect it.
        val move = obtainMultiTouch(
            MotionEvent.ACTION_MOVE,
            listOf(Triple(0, 110f + slop + 40, 260f), Triple(1, 20f, 20f)),
        )
        assertTrue(view.dispatchTouchEvent(move))
        move.recycle()
        assertEquals(afterFirstMoveX + 20, overlay.spec.x)
        assertEquals(afterFirstMoveY, overlay.spec.y)
    }

    @Test fun activePointerLiftingRebasesToTheRemainingPointerWithoutAJump() {
        val view = PositionedView(100, 240, 24)
        val overlay = OverlayView(
            view,
            OverlayScope.ACTIVITY,
            RecordingBackend(),
            OverlaySpec(touchMode = OverlayTouchMode.DRAGGABLE),
        )
        overlay.show()
        val slop = touchSlop(view)

        assertFalse(dispatch(view, MotionEvent.ACTION_DOWN, 110f, 260f))
        assertTrue(dispatch(view, MotionEvent.ACTION_MOVE, 110f + slop + 20, 260f))
        val beforeLiftX = overlay.spec.x
        val beforeLiftY = overlay.spec.y

        // Pointer 1 (far away) touches down, then the original pointer 0 lifts. Tracking must
        // rebase onto pointer 1's current position, not jump to it based on a stale delta.
        val pointerDown = obtainMultiTouch(
            MotionEvent.ACTION_POINTER_DOWN,
            listOf(Triple(0, 110f + slop + 20, 260f), Triple(1, 700f, 500f)),
            actionIndex = 1,
        )
        view.dispatchTouchEvent(pointerDown)
        pointerDown.recycle()

        val pointerUp = obtainMultiTouch(
            MotionEvent.ACTION_POINTER_UP,
            listOf(Triple(0, 110f + slop + 20, 260f), Triple(1, 700f, 500f)),
            actionIndex = 0,
        )
        assertTrue(view.dispatchTouchEvent(pointerUp))
        pointerUp.recycle()
        assertEquals(beforeLiftX, overlay.spec.x)
        assertEquals(beforeLiftY, overlay.spec.y)

        // Only pointer 1 remains; its own subsequent delta must drive the drag without a jump.
        val move = obtainMultiTouch(MotionEvent.ACTION_MOVE, listOf(Triple(1, 715f, 505f)))
        assertTrue(view.dispatchTouchEvent(move))
        move.recycle()
        assertEquals(beforeLiftX + 15, overlay.spec.x)
        assertEquals(beforeLiftY + 5, overlay.spec.y)
    }

    @Test fun preDragPointerHandoffRebasesTheSlopOriginSoAStationaryMoveStartsNoDrag() {
        val view = PositionedView(100, 240, 24)
        val overlay = OverlayView(
            view,
            OverlayScope.ACTIVITY,
            RecordingBackend(),
            OverlaySpec(touchMode = OverlayTouchMode.DRAGGABLE),
        )
        overlay.show()
        var clicks = 0
        view.setOnClickListener { clicks++ }
        val before = overlay.spec

        assertFalse(dispatch(view, MotionEvent.ACTION_DOWN, 0f, 0f))
        val pointerDown = obtainMultiTouch(
            MotionEvent.ACTION_POINTER_DOWN,
            listOf(Triple(0, 0f, 0f), Triple(1, 900f, 900f)),
            actionIndex = 1,
        )
        view.dispatchTouchEvent(pointerDown)
        pointerDown.recycle()

        // Pointer 0 (at the original slop origin) lifts before slop; pointer 1 becomes active.
        val pointerUp = obtainMultiTouch(
            MotionEvent.ACTION_POINTER_UP,
            listOf(Triple(0, 0f, 0f), Triple(1, 900f, 900f)),
            actionIndex = 0,
        )
        view.dispatchTouchEvent(pointerUp)
        pointerUp.recycle()

        // A stationary move from the new active pointer must not be measured against pointer 0's
        // original (0,0) down point -- which would spuriously already exceed slop -- so no drag
        // starts and the spec/click state are untouched.
        val move = obtainMultiTouch(MotionEvent.ACTION_MOVE, listOf(Triple(1, 900f, 900f)))
        assertFalse(view.dispatchTouchEvent(move))
        move.recycle()
        assertEquals(before, overlay.spec)

        assertFalse(dispatch(view, MotionEvent.ACTION_UP, 900f, 900f))
        assertEquals(1, clicks)
    }

    @Test fun preDragPointerHandoffThenMovingBeyondSlopStartsTheDragFromTheHandoffPosition() {
        val view = PositionedView(100, 240, 24)
        val overlay = OverlayView(
            view,
            OverlayScope.ACTIVITY,
            RecordingBackend(),
            OverlaySpec(touchMode = OverlayTouchMode.DRAGGABLE),
        )
        overlay.show()
        val slop = touchSlop(view)

        assertFalse(dispatch(view, MotionEvent.ACTION_DOWN, 0f, 0f))
        val pointerDown = obtainMultiTouch(
            MotionEvent.ACTION_POINTER_DOWN,
            listOf(Triple(0, 0f, 0f), Triple(1, 900f, 900f)),
            actionIndex = 1,
        )
        view.dispatchTouchEvent(pointerDown)
        pointerDown.recycle()
        val pointerUp = obtainMultiTouch(
            MotionEvent.ACTION_POINTER_UP,
            listOf(Triple(0, 0f, 0f), Triple(1, 900f, 900f)),
            actionIndex = 0,
        )
        view.dispatchTouchEvent(pointerUp)
        pointerUp.recycle()

        // The new active pointer now moves beyond slop measured from the handoff position
        // (900,900), not from pointer 0's original (0,0) down point.
        val move = obtainMultiTouch(MotionEvent.ACTION_MOVE, listOf(Triple(1, 900f + slop + 20, 900f)))
        assertTrue(view.dispatchTouchEvent(move))
        move.recycle()
        assertEquals(100 + slop + 20, overlay.spec.x)
        assertEquals(216, overlay.spec.y)
    }

    @Test fun dragWhileBackendUpdateThrowsPreservesEffectiveSpecAndContinuesFromLastAppliedPositionOnceItRecovers() {
        val view = PositionedView(100, 240, 24)
        val backend = RecordingBackend()
        val overlay = OverlayView(
            view,
            OverlayScope.ACTIVITY,
            backend,
            OverlaySpec(alpha = .9f, touchMode = OverlayTouchMode.DRAGGABLE),
        )
        overlay.show()
        val afterShow = overlay.spec
        val slop = touchSlop(view)
        backend.updateFailure = IllegalStateException("rejected")

        assertFalse(dispatch(view, MotionEvent.ACTION_DOWN, 110f, 260f))
        assertTrue(dispatch(view, MotionEvent.ACTION_MOVE, 110f + slop + 20, 260f))
        assertEquals(afterShow, overlay.spec)
        assertEquals(1, backend.updateCalls)

        // A second, still-failing move must retry the FULL cumulative delta since the down point,
        // not compound on top of the previously rejected attempt.
        assertTrue(dispatch(view, MotionEvent.ACTION_MOVE, 110f + slop + 50, 290f))
        assertEquals(afterShow, overlay.spec)
        assertEquals(2, backend.updateCalls)

        // Once the backend recovers, the very next move must land exactly where the finger now is
        // relative to the drag origin, not offset by the two failed attempts in between.
        backend.updateFailure = null
        assertTrue(dispatch(view, MotionEvent.ACTION_MOVE, 110f + slop + 60, 300f))
        assertEquals(100 + slop + 60, overlay.spec.x)
        assertEquals(256, overlay.spec.y)
        assertEquals(3, backend.updateCalls)

        assertTrue(dispatch(view, MotionEvent.ACTION_CANCEL, 110f + slop + 60, 300f))
        assertEquals(.9f, overlay.spec.alpha, 0f)
        assertEquals(100 + slop + 60, overlay.spec.x)
    }

    @Test fun dragBeyondSlopCancelsPendingClickAndLongPressSoNeitherFiresOnUp() {
        val view = PositionedView(100, 240, 24)
        val overlay = OverlayView(
            view,
            OverlayScope.ACTIVITY,
            RecordingBackend(),
            OverlaySpec(touchMode = OverlayTouchMode.DRAGGABLE),
        )
        overlay.show()
        // Attached only so the real framework posts the long-press callback through a real
        // Handler (an unattached View queues posts until attachment instead of scheduling them);
        // the library backend above is a no-op recorder, so this extra real parent does not
        // interact with it.
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        activity.findViewById<ViewGroup>(android.R.id.content).addView(view)
        var clicks = 0
        var longClicks = 0
        view.setOnClickListener { clicks++ }
        view.setOnLongClickListener { longClicks++; true }
        val slop = touchSlop(view)

        assertFalse(dispatch(view, MotionEvent.ACTION_DOWN, 110f, 260f))
        assertTrue(view.isPressed) // the view's own onTouchEvent(DOWN) ran and pressed it
        // Crossing slop must cancel the pending long-press callback and clear the press state
        // before either would fire.
        assertTrue(dispatch(view, MotionEvent.ACTION_MOVE, 110f + slop + 20, 260f))
        assertFalse(view.isPressed)
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(ViewConfiguration.getLongPressTimeout() + 50L))
        assertTrue(dispatch(view, MotionEvent.ACTION_UP, 110f + slop + 20, 260f))

        assertEquals(0, clicks)
        assertEquals(0, longClicks)
    }

    @Test fun nonClickableViewConsumesFromDownAndTheDragStillWorks() {
        val view = PositionedView(100, 240, 24)
        view.isClickable = false
        val overlay = OverlayView(
            view,
            OverlayScope.ACTIVITY,
            RecordingBackend(),
            OverlaySpec(touchMode = OverlayTouchMode.DRAGGABLE),
        )
        overlay.show()
        val slop = touchSlop(view)

        // DOWN is consumed immediately: there is no click to preserve on a non-clickable view.
        assertTrue(dispatch(view, MotionEvent.ACTION_DOWN, 110f, 260f))
        // Still below slop, but the listener keeps consuming every event of this same gesture.
        assertTrue(dispatch(view, MotionEvent.ACTION_MOVE, 110f + slop - 1, 260f))
        assertTrue(dispatch(view, MotionEvent.ACTION_MOVE, 110f + slop + 20, 260f))
        assertEquals(100 + slop + 20, overlay.spec.x)
        assertEquals(216, overlay.spec.y)
        assertTrue(dispatch(view, MotionEvent.ACTION_UP, 110f + slop + 20, 260f))
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M], manifest = Config.NONE)
    fun deniedPermission_activityDraggableShowGestureAndHideUseOnlyItsTargetWindowManager() {
        ShadowSettings.setCanDrawOverlays(false)
        val view = PositionedView(100, 240, 24)
        val backend = RecordingBackend()
        val overlay = OverlayView(
            view,
            OverlayScope.ACTIVITY,
            backend,
            OverlaySpec(touchMode = OverlayTouchMode.DRAGGABLE),
        )
        val slop = touchSlop(view)

        assertTrue(overlay.show().isSuccess)
        dispatch(view, MotionEvent.ACTION_DOWN, 110f, 260f)
        dispatch(view, MotionEvent.ACTION_MOVE, 110f + slop + 20, 260f)
        dispatch(view, MotionEvent.ACTION_UP, 110f + slop + 20, 260f)
        assertTrue(overlay.hide().isSuccess)

        assertEquals(1, backend.showCalls)
        assertEquals(2, backend.updateCalls)
        assertEquals(1, backend.hideCalls)
    }

    private fun touchSlop(view: View): Int = ViewConfiguration.get(view.context).scaledTouchSlop

    private fun dispatch(view: View, action: Int, x: Float, y: Float): Boolean =
        MotionEvent.obtain(0, 0, action, x, y, 0).let {
            val consumed = view.dispatchTouchEvent(it)
            it.recycle()
            consumed
        }

    private fun obtainMultiTouch(
        action: Int,
        points: List<Triple<Int, Float, Float>>,
        actionIndex: Int = 0,
    ): MotionEvent {
        val properties = points.map { (id, _, _) ->
            MotionEvent.PointerProperties().apply {
                this.id = id
                toolType = MotionEvent.TOOL_TYPE_FINGER
            }
        }.toTypedArray()
        val coords = points.map { (_, x, y) ->
            MotionEvent.PointerCoords().apply {
                this.x = x
                this.y = y
                pressure = 1f
                size = 1f
            }
        }.toTypedArray()
        val resolvedAction = action or (actionIndex shl MotionEvent.ACTION_POINTER_INDEX_SHIFT)
        return MotionEvent.obtain(
            0,
            0,
            resolvedAction,
            points.size,
            properties,
            coords,
            0,
            0,
            1f,
            1f,
            0,
            0,
            InputDevice.SOURCE_TOUCHSCREEN,
            0,
        )
    }

    /**
     * Clickable by default, matching the common case for a managed view and keeping most of this
     * suite focused on drag mechanics rather than the click/long-press consumption policy; tests
     * that specifically cover the non-clickable policy flip [View.setClickable] explicitly.
     */
    private class PositionedView(
        private val screenX: Int,
        private val screenY: Int,
        private val visibleFrameTop: Int,
        private val visibleFrameLeft: Int = 0,
    ) : View(RuntimeEnvironment.getApplication()) {
        init { isClickable = true }

        override fun getLocationOnScreen(outLocation: IntArray) {
            outLocation[0] = screenX
            outLocation[1] = screenY
        }

        override fun getWindowVisibleDisplayFrame(outRect: Rect) {
            outRect.set(visibleFrameLeft, visibleFrameTop, 1080, 1920)
        }
    }

    private class RecordingBackend(var updateFailure: Throwable? = null) : OverlayWindowManager() {
        var showCalls = 0
        var updateCalls = 0
        var hideCalls = 0
        var lastParams: WindowManager.LayoutParams? = null
        override fun show(view: View, params: WindowManager.LayoutParams) { showCalls++; lastParams = WindowManager.LayoutParams().also { it.copyFrom(params) } }
        override fun update(view: View, params: WindowManager.LayoutParams) {
            updateCalls++
            updateFailure?.let { throw it }
            lastParams = WindowManager.LayoutParams().also { it.copyFrom(params) }
        }
        override fun hide(view: View) { hideCalls++ }
    }
}
