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
 * Neither value isolates touches by UID; on Android 12 (API 31) and later the platform decides
 * whether a touch reaches another application, based on the overlay's opacity against
 * `InputManager.getMaximumObscuringOpacityForTouch()` (0.8 by default). Builds differ when the
 * alpha exceeds that maximum: many lower the overlay to the maximum and let the touch through, while
 * others, including the initial Android 12 release, keep it opaque and block the touch. Overlapping
 * overlays of one application can exceed the combined limit and be blocked either way.
 */
public enum class CrossUidPassThrough {
    /**
     * The default. The library makes no cross-UID claim and runs no opacity check; whether another
     * application receives the touch, and whether the overlay is dimmed, is up to the platform.
     */
    SAME_UID_ONLY,

    /**
     * Requests cross-UID pass-through. On API 31+ the library rejects a show or update whose
     * combined library-managed opacity exceeds the maximum with
     * [OverlayFailure.PASS_THROUGH_OPACITY_EXCEEDED], or with
     * [OverlayFailure.PASS_THROUGH_UNSUPPORTED] when the maximum is unavailable. Delivery is still
     * not guaranteed.
     */
    WHEN_SYSTEM_ALLOWS,
}
