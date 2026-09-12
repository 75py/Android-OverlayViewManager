/* Copyright 2026 75py. Licensed under the Apache License, Version 2.0. */
package com.nagopy.android.overlayviewmanager.internal

import android.graphics.Rect
import android.os.Build
import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Verifies [OverlayWindowFrame] faithfully forwards whatever frame `View.getWindowVisibleDisplayFrame`
 * reports, using a stubbed override (a listener/layout path test with a stubbed frame, not proof
 * that the real framework reports any particular shape -- see [OverlayWindowFrame]'s KDoc).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], manifest = Config.NONE)
class OverlayWindowFrameTest {
    @Test
    fun of_returnsTheViewsCurrentVisibleFrameWithoutCreatingAnAuxiliaryWindow() {
        val expected = Rect(120, 60, 900, 1200)
        val view = object : View(RuntimeEnvironment.getApplication()) {
            override fun getWindowVisibleDisplayFrame(outRect: Rect) {
                outRect.set(expected)
            }
        }

        val frame = OverlayWindowFrame.of(view)

        assertEquals(expected, frame)
    }
}
