package com.nagopy.android.overlayviewmanager

import android.os.Build
import android.view.View
import android.view.WindowManager
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], manifest = Config.NONE)
class OverlayViewStateMachineTest {
    @Test fun showFailure_keepsConfiguredAndRetainsOnlyFailureEnum() {
        val backend = RecordingBackend(addFailure = SecurityException("denied"))
        val overlay = overlay(backend)
        val result = overlay.show()
        assertFalse(result.isSuccess)
        assertEquals(OverlayFailure.PERMISSION_DENIED, result.failure)
        assertEquals(OverlayState.CONFIGURED, overlay.state)
        assertEquals(OverlayFailure.PERMISSION_DENIED, overlay.lastFailure)
        assertSame(backend.addFailure, result.cause)
    }

    @Test fun retryAfterFailedShow_isSynchronousAndAttachedOnlyAfterAddReturns() {
        val backend = RecordingBackend(addFailure = IllegalStateException("temporary"))
        val overlay = overlay(backend)
        overlay.show()
        backend.addFailure = null
        val retry = overlay.show()
        assertTrue(retry.isSuccess)
        assertTrue(retry.changed)
        assertEquals(2, backend.addCalls)
        assertEquals(OverlayState.ATTACHED, overlay.state)
        assertNull(overlay.lastFailure)
    }

    @Test fun failedUpdate_keepsLastAppliedSpecAndLayoutSnapshot() {
        val backend = RecordingBackend()
        val overlay = overlay(backend)
        overlay.show()
        val applied = overlay.spec
        backend.updateFailure = IllegalStateException("rejected")
        val result = overlay.update(applied.copy(x = 24, alpha = .5f))
        assertFalse(result.isSuccess)
        assertEquals(applied, overlay.spec)
        assertEquals(applied.x, backend.lastSuccessfulParams!!.x)
        assertEquals(applied.alpha, backend.lastSuccessfulParams!!.alpha, 0f)
    }

    @Test fun legacyPendingConfigurationDoesNotReplaceEffectiveSpecWhenApplyFails() {
        val backend = RecordingBackend()
        val overlay = overlay(backend)
        overlay.show()
        val applied = overlay.spec
        backend.updateFailure = IllegalStateException("rejected")
        overlay.setX(24).setAlpha(.5f)
        val result = overlay.update()
        assertFalse(result.isSuccess)
        assertEquals(applied, overlay.spec)
        assertEquals(24, overlay.pendingSpecForTesting().x)
        assertEquals(.5f, overlay.pendingSpecForTesting().alpha, 0f)
    }

    @Test fun equalUpdatesAndRepeatedShowAreNoOps() {
        val backend = RecordingBackend()
        val overlay = overlay(backend)
        overlay.show()
        assertFalse(overlay.show().changed)
        assertFalse(overlay.update(overlay.spec).changed)
        assertEquals(1, backend.addCalls)
        assertEquals(0, backend.updateCalls)
    }

    @Test fun notAttachedHide_reconcilesSuccessfullyAndRetainsDiagnostic() {
        val backend = RecordingBackend(removeFailure = IllegalArgumentException("View not attached"))
        val overlay = overlay(backend)
        overlay.show()
        val result = overlay.hide()
        assertTrue(result.isSuccess)
        assertFalse(result.changed)
        assertEquals(OverlayState.CONFIGURED, overlay.state)
        assertEquals(OverlayFailure.NOT_ATTACHED, overlay.lastFailure)
        assertNull(result.failure)
    }

    @Test fun notAttachedUpdateReconcilesButReportsTheDiagnosticFailure() {
        val backend = RecordingBackend(updateFailure = IllegalArgumentException("View not attached"))
        val overlay = overlay(backend)
        overlay.show()
        assertTrue(overlay.hasDetachListenerForTesting())
        val result = overlay.update(overlay.spec.copy(x = 8))
        assertFalse(result.isSuccess)
        assertEquals(OverlayFailure.NOT_ATTACHED, result.failure)
        assertEquals(OverlayState.CONFIGURED, overlay.state)
        assertEquals(OverlayFailure.NOT_ATTACHED, overlay.lastFailure)
        assertFalse(overlay.hasDetachListenerForTesting())
        assertTrue(overlay.show().isSuccess)
        assertTrue(overlay.hasDetachListenerForTesting())
    }

    @Test fun configuredHideAndDisposeRetainAnExistingDiagnostic() {
        val backend = RecordingBackend(addFailure = SecurityException("denied"))
        val overlay = overlay(backend)
        overlay.show()
        assertEquals(OverlayFailure.PERMISSION_DENIED, overlay.lastFailure)
        assertTrue(overlay.hide().isSuccess)
        assertEquals(OverlayFailure.PERMISSION_DENIED, overlay.lastFailure)
        assertTrue(overlay.dispose().isSuccess)
        assertEquals(OverlayState.DISPOSED, overlay.state)
        assertNull(overlay.backendForTesting())
        assertEquals(OverlayFailure.PERMISSION_DENIED, overlay.lastFailure)
        assertFalse(overlay.show().isSuccess)
        assertEquals(OverlayFailure.PERMISSION_DENIED, overlay.lastFailure)
    }

    @Test fun failedDisposeIsRetryableButSuccessfulDisposeReleasesView() {
        val backend = RecordingBackend(removeFailure = IllegalStateException("busy"))
        val overlay = overlay(backend)
        overlay.show()
        assertFalse(overlay.dispose().isSuccess)
        assertEquals(OverlayState.ATTACHED, overlay.state)
        backend.removeFailure = null
        assertTrue(overlay.dispose().isSuccess)
        assertEquals(OverlayState.DISPOSED, overlay.state)
        assertThrows(IllegalStateException::class.java) { overlay.view }
        assertEquals(overlay.spec, overlay.spec)
    }

    @Test fun backgroundMutationFailsBeforeCallingTheBackend() {
        val backend = RecordingBackend()
        val overlay = overlay(backend)
        val thrown = AtomicReference<Throwable?>()
        Thread { try { overlay.show() } catch (error: Throwable) { thrown.set(error) } }.apply { start(); join() }
        assertTrue(thrown.get() is IllegalStateException)
        assertEquals(0, backend.addCalls)
        assertEquals(OverlayState.CONFIGURED, overlay.state)
    }

    @Test fun applicationBrightnessIsRejectedForUpdateAndLegacySetter() {
        val overlay = OverlayView(
            View(RuntimeEnvironment.getApplication()), OverlayScope.APPLICATION, RecordingBackend(), OverlaySpec(),
        )
        assertThrows(IllegalArgumentException::class.java) {
            overlay.update(OverlaySpec(screenBrightness = .4f))
        }
        assertThrows(IllegalArgumentException::class.java) { overlay.setScreenBrightness(.4f) }
    }

    @Test fun draggableSpecInstallsTheBridgeAndUpdatesOnlyAfterTheListenerRuns() {
        val backend = RecordingBackend()
        val view = View(RuntimeEnvironment.getApplication())
        val overlay = OverlayView(view, OverlayScope.ACTIVITY, backend, OverlaySpec(touchMode = OverlayTouchMode.DRAGGABLE))
        overlay.show()
        val down = android.view.MotionEvent.obtain(0, 0, android.view.MotionEvent.ACTION_DOWN, 12f, 20f, 0)
        view.dispatchTouchEvent(down)
        down.recycle()
        assertEquals(1, backend.updateCalls)
        assertEquals(OverlayTouchMode.DRAGGABLE, overlay.spec.touchMode)
    }

    @Test fun failedDraggableUpdateKeepsThePriorEffectiveTouchState() {
        val backend = RecordingBackend()
        val view = View(RuntimeEnvironment.getApplication())
        val overlay = OverlayView(view, OverlayScope.ACTIVITY, backend, OverlaySpec(touchMode = OverlayTouchMode.INTERACTIVE))
        overlay.show()
        backend.updateFailure = IllegalStateException("rejected")
        assertFalse(overlay.update(overlay.spec.copy(touchMode = OverlayTouchMode.DRAGGABLE)).isSuccess)
        assertEquals(OverlayTouchMode.INTERACTIVE, overlay.spec.touchMode)
        val down = android.view.MotionEvent.obtain(0, 0, android.view.MotionEvent.ACTION_DOWN, 12f, 20f, 0)
        view.dispatchTouchEvent(down)
        down.recycle()
        assertEquals(1, backend.updateCalls)
    }

    private fun overlay(backend: RecordingBackend, spec: OverlaySpec = OverlaySpec()): OverlayView<View> = OverlayView(
        View(RuntimeEnvironment.getApplication()), OverlayScope.ACTIVITY, backend, spec,
    )

    private class RecordingBackend(
        var addFailure: Throwable? = null,
        var updateFailure: Throwable? = null,
        var removeFailure: Throwable? = null,
    ) : OverlayWindowManager() {
        var addCalls = 0
        var updateCalls = 0
        var lastSuccessfulParams: WindowManager.LayoutParams? = null
        override fun show(view: View, params: WindowManager.LayoutParams) { addCalls++; addFailure?.let { throw it }; lastSuccessfulParams = WindowManager.LayoutParams().also { it.copyFrom(params) } }
        override fun update(view: View, params: WindowManager.LayoutParams) { updateCalls++; updateFailure?.let { throw it }; lastSuccessfulParams = WindowManager.LayoutParams().also { it.copyFrom(params) } }
        override fun hide(view: View) { removeFailure?.let { throw it } }
    }
}
