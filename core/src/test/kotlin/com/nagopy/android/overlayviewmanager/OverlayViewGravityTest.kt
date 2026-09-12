/*
 * Copyright 2026 75py
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.nagopy.android.overlayviewmanager

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
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], manifest = Config.NONE)
class OverlayViewGravityTest {
    @Test fun rtlLayoutDirection_resolvesStartGravityToAbsoluteRight() {
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
        val view = View(RuntimeEnvironment.getApplication())
        view.layoutDirection = View.LAYOUT_DIRECTION_LTR
        val backend = RecordingBackend()
        val overlay = OverlayView(view, OverlayScope.ACTIVITY, backend, OverlaySpec(gravity = Gravity.TOP or Gravity.START))

        overlay.show()

        val params = backend.lastParams!!
        assertEquals(Gravity.TOP or Gravity.LEFT, params.gravity)
        assertEquals(0, params.gravity and Gravity.RELATIVE_LAYOUT_DIRECTION)
    }

    private class RecordingBackend : OverlayWindowManager() {
        var lastParams: WindowManager.LayoutParams? = null
        override fun show(view: View, params: WindowManager.LayoutParams) { lastParams = WindowManager.LayoutParams().also { it.copyFrom(params) } }
        override fun update(view: View, params: WindowManager.LayoutParams) { lastParams = WindowManager.LayoutParams().also { it.copyFrom(params) } }
        override fun hide(view: View) = Unit
    }
}
