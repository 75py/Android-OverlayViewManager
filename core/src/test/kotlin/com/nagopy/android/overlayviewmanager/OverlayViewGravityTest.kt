/*
 * Copyright 2026 75py
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.nagopy.android.overlayviewmanager

import android.content.pm.ApplicationInfo
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowManager
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Verifies the coordinate model's "resolved gravity is always absolute" guarantee through
 * [OverlayView]'s existing `GravityCompat.getAbsoluteGravity` call in `layoutParams()` -- this
 * library does not maintain a second, parallel gravity-resolution implementation for tests.
 *
 * These tests use `manifest = Config.NONE`, like the rest of this suite, so the test `Application`
 * has no manifest declaring `android:supportsRtl="true"`. `View`'s real layout-direction
 * resolution (`resolveLayoutDirection()`) gates entirely on `ApplicationInfo.FLAG_SUPPORTS_RTL`
 * (via `View.hasRtlSupport()`): without it, an explicit `layoutDirection` is silently ignored and
 * resolution always reports LTR, regardless of what was requested. Each test sets that flag
 * directly on the test `Application`'s `ApplicationInfo` before touching `layoutDirection`, which
 * is the plain-field equivalent of what a real manifest's `android:supportsRtl="true"` provides.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], manifest = Config.NONE)
class OverlayViewGravityTest {
    @Test fun rtlLayoutDirection_resolvesStartGravityToAbsoluteRight() {
        enableRtlSupport()
        val view = View(RuntimeEnvironment.getApplication())
        view.layoutDirection = View.LAYOUT_DIRECTION_RTL
        val backend = RecordingBackend()
        val overlay = OverlayView(view, OverlayScope.ACTIVITY, backend, OverlaySpec(gravity = Gravity.TOP or Gravity.START))

        overlay.show()

        val params = backend.lastParams!!
        assertEquals(Gravity.TOP or Gravity.RIGHT, params.gravity)
        assertEquals(0, params.gravity and Gravity.RELATIVE_LAYOUT_DIRECTION)
    }

    @Test fun ltrLayoutDirection_resolvesStartGravityToAbsoluteLeft() {
        enableRtlSupport()
        val view = View(RuntimeEnvironment.getApplication())
        view.layoutDirection = View.LAYOUT_DIRECTION_LTR
        val backend = RecordingBackend()
        val overlay = OverlayView(view, OverlayScope.ACTIVITY, backend, OverlaySpec(gravity = Gravity.TOP or Gravity.START))

        overlay.show()

        val params = backend.lastParams!!
        assertEquals(Gravity.TOP or Gravity.LEFT, params.gravity)
        assertEquals(0, params.gravity and Gravity.RELATIVE_LAYOUT_DIRECTION)
    }

    private fun enableRtlSupport() {
        val info = RuntimeEnvironment.getApplication().applicationInfo
        info.flags = info.flags or ApplicationInfo.FLAG_SUPPORTS_RTL
    }

    private class RecordingBackend : OverlayWindowManager() {
        var lastParams: WindowManager.LayoutParams? = null
        override fun show(view: View, params: WindowManager.LayoutParams) { lastParams = WindowManager.LayoutParams().also { it.copyFrom(params) } }
        override fun update(view: View, params: WindowManager.LayoutParams) { lastParams = WindowManager.LayoutParams().also { it.copyFrom(params) } }
        override fun hide(view: View) = Unit
    }
}
