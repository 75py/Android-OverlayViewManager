/* Copyright 2026 75py. Licensed under the Apache License, Version 2.0. */
package com.nagopy.android.overlayviewmanager.internal

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Table-driven coverage of [OverlayGeometry]'s coordinate model. These are plain, Robolectric-free
 * unit tests: every function under test takes only `Int` parameters, no `android.graphics.Rect` or
 * other Android framework type, so there is no risk of the Android SDK stub jar silently zeroing a
 * constructed framework object under plain JUnit.
 */
class OverlayGeometryTest {

    private data class FrameCase(
        val name: String,
        val frameLeft: Int,
        val frameTop: Int,
        val screenX: Int,
        val screenY: Int,
        val expectedWindowX: Int,
        val expectedWindowY: Int,
    )

    @Test
    fun screenToWindow_convertsUsingTheFramesOriginAcrossRepresentativeWindowShapes() {
        val cases = listOf(
            FrameCase("fullscreen frame origin (0,0)", frameLeft = 0, frameTop = 0, screenX = 100, screenY = 240, expectedWindowX = 100, expectedWindowY = 240),
            FrameCase("status-bar top inset only (0,84)", frameLeft = 0, frameTop = 84, screenX = 100, screenY = 240, expectedWindowX = 100, expectedWindowY = 156),
            FrameCase("multi-window/freeform, non-zero left AND top (120,60)", frameLeft = 120, frameTop = 60, screenX = 200, screenY = 300, expectedWindowX = 80, expectedWindowY = 240),
            FrameCase("rotated landscape frame, non-zero left only (84,0)", frameLeft = 84, frameTop = 0, screenX = 300, screenY = 50, expectedWindowX = 216, expectedWindowY = 50),
            FrameCase("edge-to-edge frame (top=0)", frameLeft = 0, frameTop = 0, screenX = 50, screenY = 2399, expectedWindowX = 50, expectedWindowY = 2399),
            // A location outside the frame (screenX/Y before the frame's origin) is not clamped to
            // 0: the result is negative, matching allowOutsideBounds=false's documented behavior
            // of reporting rather than clamping an out-of-frame location.
            FrameCase("outside-frame location is not clamped, result is negative", frameLeft = 120, frameTop = 60, screenX = 10, screenY = 5, expectedWindowX = -110, expectedWindowY = -55),
        )

        for (case in cases) {
            assertEquals(case.name, case.expectedWindowX, OverlayGeometry.screenToWindowX(case.screenX, case.frameLeft))
            assertEquals(case.name, case.expectedWindowY, OverlayGeometry.screenToWindowY(case.screenY, case.frameTop))
        }
    }
}
