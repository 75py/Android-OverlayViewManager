/* Copyright 2026 75py. Licensed under the Apache License, Version 2.0. */
package com.nagopy.android.overlayviewmanager.internal

import android.graphics.Rect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Table-driven coverage of [OverlayGeometry]'s coordinate model. These are plain, Robolectric-free
 * unit tests: every function under test is a pure computation over [Rect]/`Int` values.
 */
class OverlayGeometryTest {

    private data class FrameCase(
        val name: String,
        val frame: Rect,
        val screenX: Int,
        val screenY: Int,
        val expectedWindowX: Int,
        val expectedWindowY: Int,
    )

    @Test
    fun screenToWindow_convertsUsingTheFramesOriginAcrossRepresentativeWindowShapes() {
        val cases = listOf(
            FrameCase("fullscreen frame origin (0,0)", Rect(0, 0, 1080, 1920), screenX = 100, screenY = 240, expectedWindowX = 100, expectedWindowY = 240),
            FrameCase("status-bar top inset only (0,84)", Rect(0, 84, 1080, 1920), screenX = 100, screenY = 240, expectedWindowX = 100, expectedWindowY = 156),
            FrameCase("multi-window/freeform, non-zero left AND top (120,60)", Rect(120, 60, 900, 1200), screenX = 200, screenY = 300, expectedWindowX = 80, expectedWindowY = 240),
            FrameCase("rotated landscape frame, non-zero left only (84,0)", Rect(84, 0, 1920, 1080), screenX = 300, screenY = 50, expectedWindowX = 216, expectedWindowY = 50),
            FrameCase("edge-to-edge frame (top=0, bottom=display height)", Rect(0, 0, 1080, 2400), screenX = 50, screenY = 2399, expectedWindowX = 50, expectedWindowY = 2399),
        )

        for (case in cases) {
            assertEquals(case.name, case.expectedWindowX, OverlayGeometry.screenToWindowX(case.screenX, case.frame))
            assertEquals(case.name, case.expectedWindowY, OverlayGeometry.screenToWindowY(case.screenY, case.frame))
        }
    }

    @Test
    fun isWithinFrame_negativeOrOutsideFrameLocationIsReportedRatherThanClamped() {
        val frame = Rect(120, 60, 900, 1200)

        // Inside, including the top-left inclusive edge.
        assertTrue(OverlayGeometry.isWithinFrame(120, 60, frame))
        assertTrue(OverlayGeometry.isWithinFrame(500, 500, frame))

        // A negative/outside-frame location: this library reports it via isWithinFrame instead of
        // clamping x/y into range, leaving the clamping (or rejection) policy to the caller.
        assertFalse(OverlayGeometry.isWithinFrame(-10, 500, frame))
        assertFalse(OverlayGeometry.isWithinFrame(500, -10, frame))
        assertFalse(OverlayGeometry.isWithinFrame(1000, 500, frame))
        assertFalse(OverlayGeometry.isWithinFrame(500, 1500, frame))

        // The bottom-right edge is exclusive, matching Rect.contains semantics.
        assertFalse(OverlayGeometry.isWithinFrame(900, 500, frame))
        assertFalse(OverlayGeometry.isWithinFrame(500, 1200, frame))
    }
}
