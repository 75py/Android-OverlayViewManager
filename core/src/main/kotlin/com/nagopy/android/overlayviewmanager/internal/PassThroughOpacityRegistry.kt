/* Copyright 2026 75py. Licensed under the Apache License, Version 2.0. */
package com.nagopy.android.overlayviewmanager.internal

import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import java.util.IdentityHashMap

/**
 * Main-thread-only, process-wide registry of the alpha of every currently attached,
 * library-managed application-scope pass-through handle, used to compute the conservative
 * combined opacity `1 - product(1 - alpha_i)` that Android 12+'s per-UID obscuring-opacity budget
 * is checked against (see `OverlayView.admitPassThroughOpacity`). Every attached
 * `OverlaySpec.touchMode == PASS_THROUGH` application handle registers here regardless of its own
 * `CrossUidPassThrough` setting -- the platform's own budget for a UID is not aware of that
 * per-handle opt-in, only of `FLAG_NOT_TOUCHABLE` application overlays and their alpha -- while
 * only a `WHEN_SYSTEM_ALLOWS` handle ever triggers the admission check itself.
 *
 * Handles are keyed by identity, not by [Any.equals]/[Any.hashCode]: two distinct `OverlayView`
 * instances with an otherwise-equal [com.nagopy.android.overlayviewmanager.OverlaySpec] must not
 * collapse into one registry entry.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object PassThroughOpacityRegistry {
    private val alphaByHandle = IdentityHashMap<Any, Float>()

    @MainThread
    internal fun register(handle: Any, alpha: Float) {
        alphaByHandle[handle] = alpha
    }

    @MainThread
    internal fun unregister(handle: Any) {
        alphaByHandle.remove(handle)
    }

    /**
     * The conservative combined opacity if [handle] were attached/updated at [candidateAlpha],
     * covering every other currently registered handle plus this candidate -- never [handle]'s own
     * prior registered value, so an update lowering its own alpha is judged on the new value only.
     */
    @MainThread
    internal fun combinedOpacityExcluding(handle: Any, candidateAlpha: Float): Float {
        var product = 1f - candidateAlpha
        for ((key, alpha) in alphaByHandle) {
            if (key !== handle) product *= (1f - alpha)
        }
        return 1f - product
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    internal fun resetForTests() {
        alphaByHandle.clear()
    }

    @RestrictTo(RestrictTo.Scope.TESTS)
    internal fun containsForTests(handle: Any): Boolean = alphaByHandle.containsKey(handle)
}
