/* Copyright 2026 75py. Licensed under the Apache License, Version 2.0. */
package com.nagopy.android.overlayviewmanager.internal

import android.graphics.Rect
import androidx.annotation.RestrictTo

/**
 * Pure coordinate conversion from screen space into a target window's layout space, for the drag
 * gesture (see [com.nagopy.android.overlayviewmanager.DraggableOnTouchListener]).
 *
 * ## Coordinate model
 *
 * The drag gesture always applies an absolute `TOP|LEFT` gravity with zero margins (see
 * [com.nagopy.android.overlayviewmanager.DraggableOnTouchListener.onTouchDown]); this is a
 * property of the drag path specifically, not a library-wide invariant --
 * [com.nagopy.android.overlayviewmanager.OverlaySpec.gravity] otherwise accepts any gravity,
 * including `START`/`END`, which [com.nagopy.android.overlayviewmanager.OverlayView] resolves via
 * the existing `GravityCompat.getAbsoluteGravity` call in its `layoutParams()`. Under the drag
 * gesture's `TOP|LEFT` gravity, the platform interprets `x`/`y` as an offset from the **target
 * window's visible frame origin** -- `frame.left`/`frame.top`, i.e. the top-left corner of the
 * [Rect] returned by [com.nagopy.android.overlayviewmanager.internal.OverlayWindowFrame] -- and
 * *not* the display's `(0, 0)` origin, and not a fixed "status bar height". The visible frame
 * already reflects whatever offset applies to that window (status bar inset, multi-window/
 * freeform position, rotation, edge-to-edge), so a screen-space location is converted into this
 * library's layout coordinates by subtracting the frame's origin once; no additional per-inset
 * special-casing is needed or correct.
 *
 * This frame is a *positioning reference*, not a hard clamp on `x`/`y`:
 * * When the effective spec's `allowOutsideBounds` is `true`, `OverlayView.layoutParams()` adds
 *   `FLAG_LAYOUT_NO_LIMITS`/`FLAG_LAYOUT_IN_SCREEN`/`FLAG_LAYOUT_INSET_DECOR`, which let the
 *   platform honor an `x`/`y` outside `[0, frame.width)`/`[0, frame.height)` instead of clamping
 *   it; [isWithinFrame] tells a caller whether a location falls inside the frame, but this library
 *   does not itself clamp or reject an outside location computed by the drag gesture.
 * * The IME (soft keyboard) can shrink the visible frame's bottom while shown, on API levels/
 *   configurations where `getWindowVisibleDisplayFrame` accounts for it. Every overlay window here
 *   always carries `FLAG_NOT_FOCUSABLE`, so it can never itself take IME focus, but a frame
 *   queried while another window's IME is up reflects that shrunk region; a later query after the
 *   IME is dismissed reflects the restored one. The drag gesture only queries the frame at
 *   `ACTION_DOWN`, so it does not re-adjust mid-gesture if the IME toggles during a drag.
 * * Edge-to-edge is not special-cased: this library uses whatever frame
 *   `getWindowVisibleDisplayFrame` reports for the view's current window/insets configuration,
 *   consistent with every other inset source above.
 *
 * Every function here is a pure computation over [Rect]/`Int` values: none of them read a
 * [android.view.View] or call into the Android framework, so they are exercised directly by plain
 * JUnit tests.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object OverlayGeometry {

    /** Converts a screen-space X coordinate into an X relative to [frame]'s left edge. */
    internal fun screenToWindowX(screenX: Int, frame: Rect): Int = screenX - frame.left

    /** Converts a screen-space Y coordinate into a Y relative to [frame]'s top edge. */
    internal fun screenToWindowY(screenY: Int, frame: Rect): Int = screenY - frame.top

    /**
     * Returns whether the screen-space point ([screenX], [screenY]) lies inside [frame].
     *
     * Callers that build with `allowOutsideBounds = false` (the default) may use this to decide
     * whether a location should be rejected instead of clamped; this library does not clamp
     * out-of-frame locations on the caller's behalf.
     */
    internal fun isWithinFrame(screenX: Int, screenY: Int, frame: Rect): Boolean =
        screenX >= frame.left && screenX < frame.right && screenY >= frame.top && screenY < frame.bottom
}
