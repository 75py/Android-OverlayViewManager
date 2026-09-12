/*
 * Copyright 2026 75py
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.nagopy.android.overlayviewmanager

import android.os.Build
import android.view.View
import android.view.WindowManager
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings

/**
 * Covers the T05b permission-preflight consolidation: [OverlayView.show] obtains its
 * application-scope preflight through the injected [OverlayPermission] (which itself delegates to
 * [android.provider.Settings.canDrawOverlays]); [OverlayView.update]/[OverlayView.hide]/
 * [OverlayView.dispose] perform no preflight of their own -- see the KDoc on
 * [OverlayView.update].
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.M], manifest = Config.NONE)
class OverlayPermissionPreflightTest {

    @Test fun deniedShow_returnsPermissionDeniedWithConfiguredStateAndNoBackendCall() {
        ShadowSettings.setCanDrawOverlays(false)
        val backend = RecordingBackend()
        val overlay = applicationOverlay(backend)

        val result = overlay.show()

        assertFalse(result.isSuccess)
        assertEquals(OverlayFailure.PERMISSION_DENIED, result.failure)
        assertEquals(OverlayState.CONFIGURED, overlay.state)
        assertEquals(OverlayFailure.PERMISSION_DENIED, overlay.lastFailure)
        assertEquals(0, backend.addCalls)
    }

    @Test fun deniedThenGranted_explicitShowRetrySucceeds() {
        ShadowSettings.setCanDrawOverlays(false)
        val backend = RecordingBackend()
        val overlay = applicationOverlay(backend)
        assertFalse(overlay.show().isSuccess)

        ShadowSettings.setCanDrawOverlays(true)
        val retry = overlay.show()

        assertTrue(retry.isSuccess)
        assertTrue(retry.changed)
        assertEquals(OverlayState.ATTACHED, overlay.state)
        assertEquals(1, backend.addCalls)
        assertNull(overlay.lastFailure)
    }

    @Test fun grantedShowThenRevokedUpdate_backendSecurityExceptionKeepsAttachedAndLastEffectiveSpec() {
        ShadowSettings.setCanDrawOverlays(true)
        val backend = RecordingBackend()
        val overlay = applicationOverlay(backend)
        assertTrue(overlay.show().isSuccess)
        val effectiveBeforeRevoke = overlay.spec

        // Permission is revoked while attached; the platform (not a preflight) reports this on the
        // next window operation.
        ShadowSettings.setCanDrawOverlays(false)
        backend.updateFailure = SecurityException("permission revoked")

        val result = overlay.update(effectiveBeforeRevoke.copy(x = 42))

        assertFalse(result.isSuccess)
        assertEquals(OverlayFailure.PERMISSION_DENIED, result.failure)
        assertEquals(OverlayState.ATTACHED, overlay.state)
        assertEquals(OverlayFailure.PERMISSION_DENIED, overlay.lastFailure)
        assertEquals(effectiveBeforeRevoke, overlay.spec)
    }

    @Test fun revokedPermission_showOnAFreshHandleIsDenied() {
        ShadowSettings.setCanDrawOverlays(false)
        val overlay = applicationOverlay(RecordingBackend())

        assertEquals(OverlayFailure.PERMISSION_DENIED, overlay.show().failure)
        assertEquals(OverlayState.CONFIGURED, overlay.state)
    }

    @Test fun activityScope_neverConsultsPermissionEvenWhenApplicationPermissionIsDenied() {
        ShadowSettings.setCanDrawOverlays(false)
        val backend = RecordingBackend()
        val overlay = OverlayView(View(RuntimeEnvironment.getApplication()), OverlayScope.ACTIVITY, backend, OverlaySpec())

        val result = overlay.show()

        assertTrue(result.isSuccess)
        assertEquals(OverlayState.ATTACHED, overlay.state)
        assertEquals(1, backend.addCalls)
    }

    private fun applicationOverlay(backend: RecordingBackend): OverlayView<View> = OverlayView(
        View(RuntimeEnvironment.getApplication()),
        OverlayScope.APPLICATION,
        backend,
        OverlaySpec(),
    )

    private class RecordingBackend(var updateFailure: Throwable? = null) : OverlayWindowManager() {
        var addCalls = 0
        override fun show(view: View, params: WindowManager.LayoutParams) { addCalls++ }
        override fun update(view: View, params: WindowManager.LayoutParams) { updateFailure?.let { throw it } }
        override fun hide(view: View) = Unit
    }
}
