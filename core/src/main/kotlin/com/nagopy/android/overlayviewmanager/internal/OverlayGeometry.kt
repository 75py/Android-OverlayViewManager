/* Copyright 2026 75py. Licensed under the Apache License, Version 2.0. */
package com.nagopy.android.overlayviewmanager.internal

import android.view.Gravity
import android.view.View
import androidx.annotation.RestrictTo

/**
 * Coordinate conversion from screen space into a target window's layout space, for the drag
 * gesture (see [com.nagopy.android.overlayviewmanager.DraggableOnTouchListener]).
 *
 * Apart from [absoluteGravity], every parameter here is a plain `Int`, not an
 * `android.graphics.Rect`: under plain JUnit (no
 * Robolectric), Android framework classes run against the SDK stub jar, where a constructed
 * `Rect`'s fields silently read back as `0` instead of the values passed in. Taking ints keeps
 * "no Android framework calls in these functions" a real, plain-JUnit-verifiable property instead
 * of an unchecked claim; [com.nagopy.android.overlayviewmanager.internal.OverlayWindowFrame]'s
 * caller extracts `left`/`top` from its `Rect` before calling in here.
 *
 * ## Coordinate model (drag path only)
 *
 * Without `allowOutsideBounds`, the drag gesture normalizes the window to an absolute `TOP|LEFT`
 * gravity with zero margins (see
 * [com.nagopy.android.overlayviewmanager.DraggableOnTouchListener.onActionMove]); this is a
 * property of the drag path specifically, not a library-wide invariant --
 * [com.nagopy.android.overlayviewmanager.OverlaySpec.gravity] otherwise accepts any gravity,
 * including `START`/`END`. Under that gravity, `x`/`y` are `screenX - frame.left` /
 * `screenY - frame.top`, where `frame` is the [android.graphics.Rect] returned by
 * [com.nagopy.android.overlayviewmanager.internal.OverlayWindowFrame]. Each new gesture converts
 * from the window's real location, so a window the system clamped to its frame neither jumps nor
 * leaves a dead zone.
 *
 * With `allowOutsideBounds`, the frame is only a placeholder rectangle (see
 * [com.nagopy.android.overlayviewmanager.internal.OverlayWindowFrame]) and cannot give an origin.
 * The system does not clamp such a window either, so the spec's offsets always describe its real
 * position. The drag therefore keeps gravity and margins and adds the pointer's delta to `x`/`y`
 * directly. Offsets are measured inward from the anchored edge, so they grow leftward under right
 * gravity and upward under bottom gravity; [horizontalOffsetSign] and [verticalOffsetSign] invert
 * the delta there. Fill gravity stretches the window, so an offset does not move it on this path.
 *
 * IME visibility and edge-to-edge are observable constraints on what the frame contains at query
 * time, not behavior this function verifies or special-cases: this library does not itself clamp
 * an out-of-frame `x`/`y`, and unit tests do not exercise real framework insets -- see
 * [com.nagopy.android.overlayviewmanager.internal.OverlayWindowFrame]'s KDoc for what is and is
 * not covered by the test suite versus deferred to device/instrumentation validation.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object OverlayGeometry {

    /** Converts a screen-space X coordinate into an X relative to the frame's left edge. */
    internal fun screenToWindowX(screenX: Int, frameLeft: Int): Int = screenX - frameLeft

    /** Converts a screen-space Y coordinate into a Y relative to the frame's top edge. */
    internal fun screenToWindowY(screenY: Int, frameTop: Int): Int = screenY - frameTop

    /** Returns the sign that turns a rightward pointer delta into a change of `x`. */
    internal fun horizontalOffsetSign(absoluteGravity: Int): Int =
        if (absoluteGravity and Gravity.HORIZONTAL_GRAVITY_MASK == Gravity.RIGHT) -1 else 1

    /** Returns the sign that turns a downward pointer delta into a change of `y`. */
    internal fun verticalOffsetSign(absoluteGravity: Int): Int =
        if (absoluteGravity and Gravity.VERTICAL_GRAVITY_MASK == Gravity.BOTTOM) -1 else 1

    /**
     * Resolves [gravity] against [view]'s layout direction. A view that inherits its direction has
     * no resolved direction until it is attached, and `getLayoutDirection` then reports LTR, so
     * the first show falls back to the configuration the view will inherit.
     */
    internal fun absoluteGravity(gravity: Int, view: View): Int {
        val direction = if (view.isLayoutDirectionResolved) {
            view.layoutDirection
        } else {
            view.resources.configuration.layoutDirection
        }
        return Gravity.getAbsoluteGravity(gravity, direction)
    }
}
