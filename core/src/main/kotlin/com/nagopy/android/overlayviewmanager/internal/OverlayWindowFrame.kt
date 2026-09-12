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
 * Coverage is not one uniform setup: the frame-adapter and drag-path fixture tests
 * (`OverlayWindowFrameTest`, `DraggableOnTouchListenerTest`) run under Robolectric SDK 28 with a
 * supplied frame value (a fake `View.getWindowVisibleDisplayFrame`), not the real platform
 * implementation; `OverlayGeometryTest` is plain JUnit over `Int`s, with no Android type involved
 * at all; and `OverlayPermissionPreflightTest`, plus one `DraggableOnTouchListenerTest` case, run
 * under Robolectric SDK 23. That proves the drag-path arithmetic (`OverlayGeometry`) and the
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
 * `WindowManager`, and which window that describes depends on which `Context` supplied it. This
 * library's scope ([com.nagopy.android.overlayviewmanager.OverlayScope.ACTIVITY] vs
 * [com.nagopy.android.overlayviewmanager.OverlayScope.APPLICATION]) does not constrain the
 * concrete type of `view.context`, so which window `currentWindowMetrics` would describe is not
 * something this library can verify from scope alone. A second derivation whose correctness would
 * depend on unverified context identity is not a safe cross-check to add, so
 * [View.getWindowVisibleDisplayFrame] remains the only source.
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
