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

/**
 * Controls whether the library checks an application [OverlayTouchMode.PASS_THROUGH] overlay
 * against Android's cross-UID touch opacity limit before attaching it.
 *
 * Neither value blocks touches from reaching another application. On Android 12 (API 31) and
 * later, the platform lowers a non-touchable application overlay whose alpha exceeds
 * `InputManager.getMaximumObscuringOpacityForTouch()` (0.8 by default) to that maximum and lets
 * touches through.
 */
public enum class CrossUidPassThrough {
    /**
     * The default. The library makes no cross-UID claim and runs no opacity check; the platform
     * may still deliver touches to another application and dim the overlay as described above.
     */
    SAME_UID_ONLY,

    /**
     * Requests cross-UID pass-through. On API 31+ the library rejects a show or update with
     * [OverlayFailure.PASS_THROUGH_OPACITY_EXCEEDED] instead of letting the platform dim the
     * overlay, or with [OverlayFailure.PASS_THROUGH_UNSUPPORTED] when the maximum is unavailable.
     */
    WHEN_SYSTEM_ALLOWS,
}
