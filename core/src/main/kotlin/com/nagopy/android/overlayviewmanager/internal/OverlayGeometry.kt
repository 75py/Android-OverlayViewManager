/* Copyright 2026 75py. Licensed under the Apache License, Version 2.0. */
package com.nagopy.android.overlayviewmanager.internal

import androidx.annotation.RestrictTo

/**
 * Pure coordinate conversion from screen space into a target window's layout space, for the drag
 * gesture (see [com.nagopy.android.overlayviewmanager.DraggableOnTouchListener]).
 *
 * Every parameter here is a plain `Int`, not an `android.graphics.Rect`: under plain JUnit (no
 * Robolectric), Android framework classes run against the SDK stub jar, where a constructed
 * `Rect`'s fields silently read back as `0` instead of the values passed in. Taking ints keeps
 * "no Android framework calls in these functions" a real, plain-JUnit-verifiable property instead
 * of an unchecked claim; [com.nagopy.android.overlayviewmanager.internal.OverlayWindowFrame]'s
 * caller extracts `left`/`top` from its `Rect` before calling in here.
 *
 * ## Coordinate model (drag path only)
 *
 * The drag gesture always applies an absolute `TOP|LEFT` gravity with zero margins (see
 * [com.nagopy.android.overlayviewmanager.DraggableOnTouchListener.onActionMove]); this is a
 * property of the drag path specifically, not a library-wide invariant --
 * [com.nagopy.android.overlayviewmanager.OverlaySpec.gravity] otherwise accepts any gravity,
 * including `START`/`END`, which [com.nagopy.android.overlayviewmanager.OverlayView] resolves via
 * the existing `GravityCompat.getAbsoluteGravity` call in its `layoutParams()`. Under the drag
 * gesture's `TOP|LEFT` gravity, `x`/`y` are `screenX - frame.left` / `screenY - frame.top`, where
 * `frame` is the [android.graphics.Rect] returned by
 * [com.nagopy.android.overlayviewmanager.internal.OverlayWindowFrame].
 *
 * `allowOutsideBounds`/IME visibility/edge-to-edge are observable constraints on what that frame
 * contains at query time, not behavior this function verifies or special-cases: this library does
 * not itself clamp an out-of-frame `x`/`y`, and unit tests do not exercise real framework insets --
 * see [com.nagopy.android.overlayviewmanager.internal.OverlayWindowFrame]'s KDoc for what is and
 * is not covered by the test suite versus deferred to device/instrumentation validation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object OverlayGeometry {

    /** Converts a screen-space X coordinate into an X relative to the frame's left edge. */
    internal fun screenToWindowX(screenX: Int, frameLeft: Int): Int = screenX - frameLeft

    /** Converts a screen-space Y coordinate into a Y relative to the frame's top edge. */
    internal fun screenToWindowY(screenY: Int, frameTop: Int): Int = screenY - frameTop
}
