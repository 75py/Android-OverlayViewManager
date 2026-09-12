/* Copyright 2026 75py. Licensed under the Apache License, Version 2.0. */
package com.nagopy.android.overlayviewmanager.internal

import android.graphics.Rect
import android.view.View
import androidx.annotation.RestrictTo

/**
 * Obtains the visible frame of the window that currently hosts a managed [View], for use with
 * [OverlayGeometry]'s drag-path coordinate model. [View.getWindowVisibleDisplayFrame] is the
 * chosen positioning reference: it works against the view's own window without creating a second/
 * auxiliary window and without requiring `SYSTEM_ALERT_WINDOW` for the activity scope.
 *
 * ## What is and is not verified by this module's unit tests
 *
 * The unit test suite runs entirely under Robolectric on a single SDK (28) with the frame value
 * *overridden* by the test fixture (a fake `View.getWindowVisibleDisplayFrame`), not the real
 * platform implementation. That proves the drag-path arithmetic (`OverlayGeometry`) and the
 * listener/layout path correctly consume whatever frame this adapter returns; it does not prove
 * that the real framework's `getWindowVisibleDisplayFrame` is itself equivalent across API 23,
 * 26, 35, 36, multi-window/freeform, RTL, or edge-to-edge configurations. That real-framework
 * equivalence is deferred to the T12 device/instrumentation validation matrix.
 *
 * ## Why not also cross-check with `WindowManager.getCurrentWindowMetrics()` (API 30+)
 *
 * `getCurrentWindowMetrics()` returns the *current window's own* bounds and
 * [android.view.WindowInsets] -- not the display's -- so it is not inherently wrong as a source.
 * It is deliberately not used here, even as an API 30+ cross-check, because it is obtained from a
 * `WindowManager`, and which window that describes depends on which `Context` supplied it -- a
 * distinction that varies by this library's scope: for
 * [com.nagopy.android.overlayviewmanager.OverlayScope.ACTIVITY], `view.context`'s `WindowManager`
 * is the hosting Activity's own; for
 * [com.nagopy.android.overlayviewmanager.OverlayScope.APPLICATION], the overlay view's context has
 * no windowed Activity backing it, so the `WindowManager` obtained from it describes the
 * *default-display* window rather than the overlay's own window. A second derivation whose own
 * correctness would need to vary by scope is not a safe cross-check to add without the same
 * device/instrumentation validation noted above, so [View.getWindowVisibleDisplayFrame] remains
 * the only source.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object OverlayWindowFrame {
    /** Returns the visible frame, in screen coordinates, of the window hosting [view]. */
    internal fun of(view: View): Rect {
        val frame = Rect()
        view.getWindowVisibleDisplayFrame(frame)
        return frame
    }
}
