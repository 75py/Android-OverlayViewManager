/*
 * Copyright 2026 75py
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.nagopy.android.overlayviewmanager

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/**
 * Immutable, thread-safe access to the "draw over other apps" permission.
 *
 * Every method takes the [Context] it needs and this class holds no state of its own, so an
 * instance may be queried from any thread. The library's minSdk is API 23, so [isGranted] always
 * delegates to [Settings.canDrawOverlays]; there is no lower-API branch.
 */
public class OverlayPermission {
    /** Returns whether [context]'s package currently holds the overlay permission. */
    public fun isGranted(context: Context): Boolean = Settings.canDrawOverlays(context)

    /**
     * Returns a new [Settings.ACTION_MANAGE_OVERLAY_PERMISSION] intent for [context]'s package.
     * This does not start an activity and has no Fragment/Dialog dependency; the host launches it,
     * for example through the Activity Result API, and rechecks [isGranted] on return.
     */
    public fun settingsIntent(context: Context): Intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        Uri.parse("package:${context.packageName}"),
    )
}
