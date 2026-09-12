/* Copyright 2026 75py. Licensed under the Apache License, Version 2.0. */
package com.nagopy.android.overlayviewmanager.internal

import android.graphics.Rect
import android.view.View
import androidx.annotation.RestrictTo

/**
 * Obtains the visible frame of the window that currently hosts a managed [View], for use with
 * [OverlayGeometry]'s drag-path coordinate model. This is the sole source of window bounds/insets
 * for the drag gesture: it never creates a second/auxiliary window and never requires
 * `SYSTEM_ALERT_WINDOW` for the activity scope, because [View.getWindowVisibleDisplayFrame] works
 * against the view's own window on every supported API level (23+).
 *
 * ## Why not also cross-check with `WindowManager.getCurrentWindowMetrics()` (API 30+)
 *
 * `getCurrentWindowMetrics()` returns the *current window's own* bounds and
 * [android.view.WindowInsets] -- not the display's -- so it is not inherently wrong as a source.
 * It is deliberately not used here, even as an API 30+ cross-check, because it is obtained from a
 * `WindowManager`, and which window that describes depends on which `Context` supplied it -- a
 * distinction that varies by this library's scope:
 * * For [com.nagopy.android.overlayviewmanager.OverlayScope.ACTIVITY], `view.context`'s
 *   `WindowManager` is the hosting Activity's own, so `currentWindowMetrics` is task-bounded and
 *   would likely agree with [View.getWindowVisibleDisplayFrame].
 * * For [com.nagopy.android.overlayviewmanager.OverlayScope.APPLICATION], the overlay view's
 *   context has no windowed Activity backing it, so the `WindowManager` obtained from it is the
 *   *default-display* window manager; its `currentWindowMetrics` describes the display's window,
 *   not necessarily the overlay's own `TYPE_APPLICATION_OVERLAY`/`TYPE_SYSTEM_ALERT` window. Using
 *   it here would reintroduce the same scope-dependent display-vs-window ambiguity that
 *   `OverlayViewManager.getDisplayWidth/Height()` was deprecated for.
 *
 * A second derivation whose own correctness would need to vary by scope is not a safe or provably
 * equivalent cross-check to add without a real-device/instrumented matrix across both scopes and
 * every supported API level -- which is out of scope here. [View.getWindowVisibleDisplayFrame]
 * alone is already correct and consistent across both scopes on every supported API level, so it
 * remains the only source; the coordinate-model tests instead verify its output end-to-end through
 * the real drag/layout path (see `DraggableOnTouchListenerTest` and `OverlayViewGravityTest`).
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
