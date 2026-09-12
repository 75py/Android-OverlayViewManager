package com.nagopy.android.overlayviewmanager

import android.os.Build
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/** Covers the temporary listener bridge without reintroducing mutable LayoutParams access. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], manifest = Config.NONE)
class DraggableOnTouchListenerTest {
    @Test fun upRestoresTheEffectiveAlphaThroughTheImmutableSpecBridge() {
        val view = View(RuntimeEnvironment.getApplication())
        val overlay = OverlayView(view, OverlayScope.ACTIVITY, NoOpBackend(), OverlaySpec(alpha = .5f))
        overlay.show()
        val listener = DraggableOnTouchListener(overlay)
        val event = MotionEvent.obtain(0, 0, MotionEvent.ACTION_UP, 0f, 0f, 0)

        assertFalse(listener.onTouch(view, event))
        assertEquals(.5f, overlay.spec.alpha, 0f)
        event.recycle()
    }

    private class NoOpBackend : OverlayWindowManager() {
        override fun show(view: View, params: WindowManager.LayoutParams) = Unit
        override fun update(view: View, params: WindowManager.LayoutParams) = Unit
        override fun hide(view: View) = Unit
    }
}
