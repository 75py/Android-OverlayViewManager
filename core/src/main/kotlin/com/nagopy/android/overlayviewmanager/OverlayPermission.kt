/*
 * Copyright 2026 75py
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.nagopy.android.overlayviewmanager

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/** Immutable application-scoped access to overlay permission state and settings navigation. */
public class OverlayPermission private constructor(
    private val applicationContext: Context,
    private val packageName: String,
) {
    /** Returns the platform's current overlay permission state. */
    public fun isGranted(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.M || Settings.canDrawOverlays(applicationContext)

    /** Returns a new settings intent without launching it. */
    public fun settingsIntent(): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:$packageName"),
    )

    internal companion object {
        @JvmSynthetic
        internal fun create(application: Application): OverlayPermission = OverlayPermission(
            application.applicationContext,
            application.packageName,
        )
    }
}
