package com.nagopy.android.overlayviewmanager

import android.graphics.Rect
import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings

/** Covers the temporary listener bridge without reintroducing mutable LayoutParams access. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], manifest = Config.NONE)
class DraggableOnTouchListenerTest {
    @Test fun downMoveUpPreservesDefaultListenerGestureStateAndRestoresAlpha() {
        val view = PositionedView(100, 240, 24)
        val backend = RecordingBackend()
        val overlay = OverlayView(
            view,
            OverlayScope.ACTIVITY,
            backend,
            OverlaySpec(alpha = .8f, touchMode = OverlayTouchMode.DRAGGABLE),
        )
        overlay.show()
        val installed = privateField(overlay, "effectiveDragListener")

        dispatch(view, MotionEvent.ACTION_DOWN, 110f, 260f)
        assertEquals(100, overlay.spec.x)
        assertEquals(216, overlay.spec.y)
        assertEquals(.48f, overlay.spec.alpha, .0001f)
        assertSame(installed, privateField(overlay, "effectiveDragListener"))
        dispatch(view, MotionEvent.ACTION_MOVE, 130f, 290f)
        assertEquals(120, overlay.spec.x)
        assertEquals(246, overlay.spec.y)
        assertSame(installed, privateField(overlay, "effectiveDragListener"))
        dispatch(view, MotionEvent.ACTION_UP, 130f, 290f)
        assertEquals(.8f, overlay.spec.alpha, 0f)
        assertSame(installed, privateField(overlay, "effectiveDragListener"))
        assertEquals(3, backend.updateCalls)
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

        dispatch(view, MotionEvent.ACTION_DOWN, 10f, 30f)
        assertEquals(50, overlay.spec.x)
        assertEquals(140, overlay.spec.y)
        dispatch(view, MotionEvent.ACTION_MOVE, 20f, 55f)
        assertEquals(60, overlay.spec.x)
        assertEquals(165, overlay.spec.y)
        dispatch(view, MotionEvent.ACTION_CANCEL, 20f, 55f)
        assertEquals(60, overlay.spec.x)
        assertEquals(165, overlay.spec.y)
        assertEquals(.75f, overlay.spec.alpha, 0f)
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

        assertTrue(overlay.show().isSuccess)
        dispatch(view, MotionEvent.ACTION_DOWN, 110f, 260f)
        dispatch(view, MotionEvent.ACTION_UP, 110f, 260f)
        assertTrue(overlay.hide().isSuccess)

        assertEquals(1, backend.showCalls)
        assertEquals(2, backend.updateCalls)
        assertEquals(1, backend.hideCalls)
    }

    private fun dispatch(view: View, action: Int, x: Float, y: Float) {
        MotionEvent.obtain(0, 0, action, x, y, 0).also {
            assertFalse(view.dispatchTouchEvent(it))
            it.recycle()
        }
    }

    private fun privateField(instance: Any, name: String): Any? = instance.javaClass.getDeclaredField(name).let {
        it.isAccessible = true
        it.get(instance)
    }

    private class PositionedView(
        private val screenX: Int,
        private val screenY: Int,
        private val visibleFrameTop: Int,
    ) : View(RuntimeEnvironment.getApplication()) {
        override fun getLocationOnScreen(outLocation: IntArray) {
            outLocation[0] = screenX
            outLocation[1] = screenY
        }

        override fun getWindowVisibleDisplayFrame(outRect: Rect) {
            outRect.set(0, visibleFrameTop, 1080, 1920)
        }
    }

    private class RecordingBackend : OverlayWindowManager() {
        var showCalls = 0
        var updateCalls = 0
        var hideCalls = 0
        override fun show(view: View, params: WindowManager.LayoutParams) { showCalls++ }
        override fun update(view: View, params: WindowManager.LayoutParams) { updateCalls++ }
        override fun hide(view: View) { hideCalls++ }
    }
}
