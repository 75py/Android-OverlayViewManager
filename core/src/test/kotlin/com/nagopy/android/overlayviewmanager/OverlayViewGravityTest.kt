/*
 * Copyright 2026 75py
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.nagopy.android.overlayviewmanager

import android.app.Activity
import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.res.Configuration
import android.os.Build
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowManager
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Verifies the coordinate model's "resolved gravity is always absolute" guarantee through
 * [OverlayView]'s `layoutParams()`, which resolves gravity with `OverlayGeometry.absoluteGravity`.
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
        val overlay = OverlayView.create(view, OverlayScope.ACTIVITY, backend, OverlaySpec(gravity = Gravity.TOP or Gravity.START))

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
        val overlay = OverlayView.create(view, OverlayScope.ACTIVITY, backend, OverlaySpec(gravity = Gravity.TOP or Gravity.START))

        overlay.show()

        val params = backend.lastParams!!
        assertEquals(Gravity.TOP or Gravity.LEFT, params.gravity)
        assertEquals(0, params.gravity and Gravity.RELATIVE_LAYOUT_DIRECTION)
    }

    @Test fun inheritedRtlConfiguration_resolvesStartGravityBeforeTheViewIsAttached() {
        enableRtlSupport()
        val view = View(rtlContext())
        val backend = RecordingBackend()
        val overlay = OverlayView.create(view, OverlayScope.ACTIVITY, backend, OverlaySpec(gravity = Gravity.TOP or Gravity.START))

        overlay.show()

        assertEquals(Gravity.TOP or Gravity.RIGHT, backend.lastParams!!.gravity)
    }

    @Test fun rtlConfiguration_isIgnoredWhenTheApplicationDoesNotSupportRtl() {
        val view = View(rtlContext())
        val backend = RecordingBackend()
        val overlay = OverlayView.create(view, OverlayScope.ACTIVITY, backend, OverlaySpec(gravity = Gravity.TOP or Gravity.START))

        overlay.show()

        assertEquals(Gravity.TOP or Gravity.LEFT, backend.lastParams!!.gravity)
    }

    @Test fun declaredLtrInAnRtlConfiguration_isCorrectedAfterTheFirstLayout() {
        val view = viewDeclaringLtrInAnRtlConfiguration()
        val backend = RecordingBackend()
        val overlay = OverlayView.create(view, OverlayScope.ACTIVITY, backend, OverlaySpec(gravity = Gravity.TOP or Gravity.START))

        overlay.show()
        // The declared direction cannot be read yet, so the first show uses the configuration.
        assertEquals(Gravity.TOP or Gravity.RIGHT, backend.lastParams!!.gravity)

        layOut(view)

        assertEquals(Gravity.TOP or Gravity.LEFT, backend.lastParams!!.gravity)
        assertEquals(1, backend.updateCalls)
    }

    @Test fun hideBeforeTheFirstLayout_removesTheCorrection() {
        val view = viewDeclaringLtrInAnRtlConfiguration()
        val backend = RecordingBackend()
        val overlay = OverlayView.create(view, OverlayScope.ACTIVITY, backend, OverlaySpec(gravity = Gravity.TOP or Gravity.START))

        overlay.show()
        overlay.hide()
        layOut(view)

        assertEquals(0, backend.updateCalls)
    }

    @Test fun disposeBeforeTheFirstLayout_removesTheCorrection() {
        val view = viewDeclaringLtrInAnRtlConfiguration()
        val backend = RecordingBackend()
        val overlay = OverlayView.create(view, OverlayScope.ACTIVITY, backend, OverlaySpec(gravity = Gravity.TOP or Gravity.START))

        overlay.show()
        overlay.dispose()
        layOut(view)

        assertEquals(0, backend.updateCalls)
    }

    @Test fun showHideShow_appliesOneCorrection() {
        val view = viewDeclaringLtrInAnRtlConfiguration()
        val backend = RecordingBackend()
        val overlay = OverlayView.create(view, OverlayScope.ACTIVITY, backend, OverlaySpec(gravity = Gravity.TOP or Gravity.START))

        overlay.show()
        overlay.hide()
        overlay.show()
        layOut(view)

        assertEquals(Gravity.TOP or Gravity.LEFT, backend.lastParams!!.gravity)
        assertEquals(1, backend.updateCalls)
    }

    @Test fun externalDetachBeforeTheFirstLayout_removesTheCorrection() {
        val view = viewDeclaringLtrInAnRtlConfiguration()
        val backend = RecordingBackend()
        val overlay = OverlayView.create(view, OverlayScope.ACTIVITY, backend, OverlaySpec(gravity = Gravity.TOP or Gravity.START))
        overlay.show()
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val content = activity.findViewById<ViewGroup>(android.R.id.content)

        content.addView(view)
        content.removeView(view)
        assertEquals(OverlayState.CONFIGURED, overlay.state)
        layOut(view)

        assertEquals(0, backend.updateCalls)
    }

    @Test fun failedCorrection_isClassifiedAndKeepsTheOverlayAttached() {
        val view = viewDeclaringLtrInAnRtlConfiguration()
        val backend = RecordingBackend()
        val overlay = OverlayView.create(view, OverlayScope.ACTIVITY, backend, OverlaySpec(gravity = Gravity.TOP or Gravity.START))
        overlay.show()
        backend.updateFailure = SecurityException("denied")

        layOut(view)

        assertEquals(OverlayFailure.PERMISSION_DENIED, overlay.lastFailure)
        assertEquals(OverlayState.ATTACHED, overlay.state)
    }

    @Test fun correctionOfADetachedWindow_reconcilesTheState() {
        val view = viewDeclaringLtrInAnRtlConfiguration()
        val backend = RecordingBackend()
        val overlay = OverlayView.create(view, OverlayScope.ACTIVITY, backend, OverlaySpec(gravity = Gravity.TOP or Gravity.START))
        overlay.show()
        backend.updateFailure = IllegalArgumentException("View not attached to window manager")

        layOut(view)

        assertEquals(OverlayFailure.NOT_ATTACHED, overlay.lastFailure)
        assertEquals(OverlayState.CONFIGURED, overlay.state)
    }

    @Test fun inheritedDirection_needsNoCorrectionAfterTheFirstLayout() {
        enableRtlSupport()
        val view = View(RuntimeEnvironment.getApplication())
        val backend = RecordingBackend()
        val overlay = OverlayView.create(view, OverlayScope.ACTIVITY, backend, OverlaySpec(gravity = Gravity.TOP or Gravity.START))

        overlay.show()
        layOut(view)

        assertEquals(0, backend.updateCalls)
    }

    private fun viewDeclaringLtrInAnRtlConfiguration(): View {
        enableRtlSupport()
        val attributes = Robolectric.buildAttributeSet()
            .addAttribute(android.R.attr.layoutDirection, "ltr")
            .build()
        return View(rtlContext(), attributes)
    }

    private fun rtlContext(): Context {
        val application = RuntimeEnvironment.getApplication()
        val rtl = Configuration(application.resources.configuration).apply { setLocale(Locale("ar")) }
        return application.createConfigurationContext(rtl)
    }

    private fun layOut(view: View) {
        view.measure(
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(100, View.MeasureSpec.EXACTLY),
        )
        view.layout(0, 0, 100, 100)
    }

    private fun enableRtlSupport() {
        val info = RuntimeEnvironment.getApplication().applicationInfo
        info.flags = info.flags or ApplicationInfo.FLAG_SUPPORTS_RTL
    }

    private class RecordingBackend : OverlayWindowManager() {
        var updateCalls = 0
        var updateFailure: Exception? = null
        var lastParams: WindowManager.LayoutParams? = null
        override fun show(view: View, params: WindowManager.LayoutParams) { lastParams = WindowManager.LayoutParams().also { it.copyFrom(params) } }
        override fun update(view: View, params: WindowManager.LayoutParams) {
            updateCalls++
            updateFailure?.let { throw it }
            lastParams = WindowManager.LayoutParams().also { it.copyFrom(params) }
        }
        override fun hide(view: View) = Unit
    }
}
