/* Copyright 2026 75py. Licensed under the Apache License, Version 2.0. */
package com.nagopy.android.overlayviewmanager.internal

import android.content.Context
import android.hardware.input.InputManager
import android.os.Build
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo

/**
 * Source of the platform's per-UID maximum obscuring opacity for touch
 * (`InputManager.getMaximumObscuringOpacityForTouch()`, API 31+), injectable so Robolectric tests
 * can supply a value without a shadow of `InputManager`. Returns `null` when the value cannot be
 * obtained -- below API 31, when the system service is unavailable, or when the query throws --
 * so the caller fails closed with `PASS_THROUGH_UNSUPPORTED` rather than skipping the check.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal fun interface MaximumObscuringOpacitySource {
    @MainThread fun maximumObscuringOpacityForTouch(context: Context): Float?
}

@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object PlatformMaximumObscuringOpacitySource : MaximumObscuringOpacitySource {
    override fun maximumObscuringOpacityForTouch(context: Context): Float? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        return try {
            context.getSystemService(InputManager::class.java)?.maximumObscuringOpacityForTouch
        } catch (exception: Exception) {
            Logger.w(exception, "Failed to read the maximum obscuring opacity for touch.")
            null
        }
    }
}

/** Process-wide, test-replaceable holder for the active [MaximumObscuringOpacitySource]. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object MaximumObscuringOpacity {
    @Volatile private var source: MaximumObscuringOpacitySource? = null

    @MainThread
    internal fun get(): MaximumObscuringOpacitySource = source ?: PlatformMaximumObscuringOpacitySource

    @RestrictTo(RestrictTo.Scope.TESTS)
    internal fun setForTests(value: MaximumObscuringOpacitySource) { source = value }
}
