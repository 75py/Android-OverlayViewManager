package com.nagopy.android.overlayviewmanager

import android.os.Build
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
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
import org.robolectric.shadows.ShadowSettings
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

    @Test fun showWithParentedViewFailsBeforeTheBackendIsCalled() {
        val backend = RecordingBackend()
        val view = View(RuntimeEnvironment.getApplication())
        val overlay = OverlayView(view, OverlayScope.ACTIVITY, backend, OverlaySpec())
        FrameLayout(view.context).addView(view)

        val result = overlay.show()

        assertEquals(OverlayFailure.ALREADY_HAS_PARENT, result.failure)
        assertEquals(OverlayState.CONFIGURED, overlay.state)
        assertEquals(OverlayFailure.ALREADY_HAS_PARENT, overlay.lastFailure)
        assertEquals(0, backend.addCalls)
    }

    @Test fun badTokenFromShowIsClassifiedWithoutAttaching() {
        val backend = RecordingBackend(addFailure = WindowManager.BadTokenException("bad token"))
        val overlay = overlay(backend)

        val result = overlay.show()

        assertEquals(OverlayFailure.INVALID_WINDOW_TOKEN, result.failure)
        assertSame(backend.addFailure, result.cause)
        assertEquals(OverlayState.CONFIGURED, overlay.state)
        assertEquals(OverlayFailure.INVALID_WINDOW_TOKEN, overlay.lastFailure)
    }

    @Test fun backendErrorPropagatesWithoutClassification() {
        val error = AssertionError("fatal")
        val backend = RecordingBackend(addFailure = error)
        val overlay = overlay(backend)

        assertSame(error, assertThrows(AssertionError::class.java) { overlay.show() })
        assertEquals(1, backend.addCalls)
        assertEquals(OverlayState.CONFIGURED, overlay.state)
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
        assertTrue(privateField(overlay, "detachListener") != null)
        val result = overlay.update(overlay.spec.copy(x = 8))
        assertFalse(result.isSuccess)
        assertEquals(OverlayFailure.NOT_ATTACHED, result.failure)
        assertEquals(OverlayState.CONFIGURED, overlay.state)
        assertEquals(OverlayFailure.NOT_ATTACHED, overlay.lastFailure)
        assertNull(privateField(overlay, "detachListener"))
        assertTrue(overlay.show().isSuccess)
        assertTrue(privateField(overlay, "detachListener") != null)
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
        assertNull(privateField(overlay, "backend"))
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

    @Test fun notAttachedDisposeReconcilesAndReleasesOwnedReferences() {
        val backend = RecordingBackend(removeFailure = IllegalArgumentException("View not attached"))
        val overlay = overlay(backend)
        overlay.show()

        val result = overlay.dispose()

        assertTrue(result.isSuccess)
        assertFalse(result.changed)
        assertNull(result.failure)
        assertEquals(OverlayState.DISPOSED, overlay.state)
        assertEquals(OverlayFailure.NOT_ATTACHED, overlay.lastFailure)
        assertNull(privateField(overlay, "backend"))
        assertNull(privateField(overlay, "ownedView"))
        assertThrows(IllegalStateException::class.java) { overlay.view }
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

    @Test fun applicationBrightnessIsRejectedForUpdate() {
        val overlay = OverlayView(
            View(RuntimeEnvironment.getApplication()), OverlayScope.APPLICATION, RecordingBackend(), OverlaySpec(),
        )
        assertThrows(IllegalArgumentException::class.java) {
            overlay.update(OverlaySpec(screenBrightness = .4f))
        }
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M], manifest = Config.NONE)
    fun applicationPermissionPreflightPreventsAddAndAllowsRetryWhileActivityScopeWorksDenied() {
        val application = RuntimeEnvironment.getApplication()
        val applicationBackend = RecordingBackend()
        ShadowSettings.setCanDrawOverlays(false)
        val applicationOverlay = OverlayView(View(application), OverlayScope.APPLICATION, applicationBackend, OverlaySpec())

        assertEquals(OverlayFailure.PERMISSION_DENIED, applicationOverlay.show().failure)
        assertEquals(0, applicationBackend.addCalls)
        ShadowSettings.setCanDrawOverlays(true)
        assertTrue(applicationOverlay.show().isSuccess)
        assertEquals(1, applicationBackend.addCalls)

        ShadowSettings.setCanDrawOverlays(false)
        val activityBackend = RecordingBackend()
        val activityOverlay = OverlayView(View(application), OverlayScope.ACTIVITY, activityBackend, OverlaySpec())
        assertTrue(activityOverlay.show().isSuccess)
        assertEquals(1, activityBackend.addCalls)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M], manifest = Config.NONE)
    fun windowTypesAndFlagsUseOnlyAllowedApplicationAndActivityValuesAtApi23Boundary() {
        ShadowSettings.setCanDrawOverlays(true)
        val application = RuntimeEnvironment.getApplication()
        val applicationBackend = RecordingBackend()
        val applicationOverlay = OverlayView(View(application), OverlayScope.APPLICATION, applicationBackend, OverlaySpec())
        applicationOverlay.show()
        assertAllowedFlags(applicationBackend.lastSuccessfulParams!!, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, passThrough = true)
        applicationOverlay.update(applicationOverlay.spec.copy(touchMode = OverlayTouchMode.INTERACTIVE))
        assertAllowedFlags(applicationBackend.lastSuccessfulParams!!, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, passThrough = false)
        applicationOverlay.update(applicationOverlay.spec.copy(touchMode = OverlayTouchMode.DRAGGABLE))
        assertAllowedFlags(applicationBackend.lastSuccessfulParams!!, WindowManager.LayoutParams.TYPE_SYSTEM_ALERT, passThrough = false)

        val activityBackend = RecordingBackend()
        val activityOverlay = OverlayView(View(application), OverlayScope.ACTIVITY, activityBackend, OverlaySpec())
        activityOverlay.show()
        assertAllowedFlags(activityBackend.lastSuccessfulParams!!, WindowManager.LayoutParams.TYPE_APPLICATION, passThrough = true)
        activityOverlay.update(activityOverlay.spec.copy(touchMode = OverlayTouchMode.INTERACTIVE))
        assertAllowedFlags(activityBackend.lastSuccessfulParams!!, WindowManager.LayoutParams.TYPE_APPLICATION, passThrough = false)
        activityOverlay.update(activityOverlay.spec.copy(touchMode = OverlayTouchMode.DRAGGABLE))
        assertAllowedFlags(activityBackend.lastSuccessfulParams!!, WindowManager.LayoutParams.TYPE_APPLICATION, passThrough = false)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O], manifest = Config.NONE)
    fun applicationWindowTypeUsesApplicationOverlayFromApi26() {
        ShadowSettings.setCanDrawOverlays(true)
        val backend = RecordingBackend()
        val overlay = OverlayView(View(RuntimeEnvironment.getApplication()), OverlayScope.APPLICATION, backend, OverlaySpec())

        assertTrue(overlay.show().isSuccess)
        assertAllowedFlags(backend.lastSuccessfulParams!!, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, passThrough = true)
    }

    @Test fun updateAppliesEveryLayoutFieldAndViewOwnsTheClickListener() {
        val backend = RecordingBackend()
        val view = View(RuntimeEnvironment.getApplication())
        val overlay = OverlayView(view, OverlayScope.ACTIVITY, backend, OverlaySpec())
        overlay.show()
        var clicks = 0
        view.setOnClickListener { clicks++ }
        overlay.update(
            overlay.spec.copy(
                width = 240,
                height = 80,
                gravity = android.view.Gravity.BOTTOM,
                x = 12,
                y = 34,
                alpha = .7f,
                horizontalMargin = .2f,
                verticalMargin = .3f,
            ),
        )

        assertEquals(240, overlay.spec.width)
        assertEquals(80, backend.lastSuccessfulParams!!.height)
        assertEquals(12, backend.lastSuccessfulParams!!.x)
        assertEquals(.7f, backend.lastSuccessfulParams!!.alpha, 0f)
        view.performClick()
        assertEquals(1, clicks)
    }

    @Test fun draggableSpecInstallsTheBridgeAndUpdatesOnlyAfterTheListenerRuns() {
        val backend = RecordingBackend()
        val view = View(RuntimeEnvironment.getApplication())
        val overlay = OverlayView(view, OverlayScope.ACTIVITY, backend, OverlaySpec(touchMode = OverlayTouchMode.DRAGGABLE))
        overlay.show()
        val slop = android.view.ViewConfiguration.get(view.context).scaledTouchSlop
        val down = android.view.MotionEvent.obtain(0, 0, android.view.MotionEvent.ACTION_DOWN, 12f, 20f, 0)
        view.dispatchTouchEvent(down)
        down.recycle()
        // The bridge installs immediately, but T06 gates the first backend call on touch slop.
        assertEquals(0, backend.updateCalls)
        val move = android.view.MotionEvent.obtain(0, 0, android.view.MotionEvent.ACTION_MOVE, 12f + slop + 10, 20f, 0)
        view.dispatchTouchEvent(move)
        move.recycle()
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

    private fun assertAllowedFlags(params: WindowManager.LayoutParams, type: Int, passThrough: Boolean) {
        assertEquals(type, params.type)
        val expectedFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            if (passThrough) WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE else 0
        assertEquals(expectedFlags, params.flags)
        assertFalse(params.type == WindowManager.LayoutParams.TYPE_SYSTEM_ERROR)
        assertFalse(params.type == WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY)
    }

    private fun privateField(instance: Any, name: String): Any? = instance.javaClass.getDeclaredField(name).let {
        it.isAccessible = true
        it.get(instance)
    }

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
