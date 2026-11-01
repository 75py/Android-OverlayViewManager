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

/** Classifies a failed overlay operation without requiring exception inspection. */
public enum class OverlayFailure {
    /** The overlay permission is missing, or the platform rejected the window with a security error. */
    PERMISSION_DENIED,

    /** [OverlayView.show] or [OverlayView.update] ran after the owning Activity's destruction disposed the handle. */
    ACTIVITY_DESTROYED,

    /** The platform rejected the window's token. */
    INVALID_WINDOW_TOKEN,

    /**
     * The window manager rejected the operation for another reason. [OverlayView.show] and
     * [OverlayView.update] also report this after an ordinary [OverlayView.dispose].
     */
    WINDOW_MANAGER_REJECTED,

    /** The managed View already has a parent, so it cannot be added to a window. */
    ALREADY_HAS_PARENT,

    /** The window was already gone. The handle reconciles to [OverlayState.CONFIGURED]. */
    NOT_ATTACHED,

    /** The combined opacity of the pass-through overlays would exceed the platform's maximum. */
    PASS_THROUGH_OPACITY_EXCEEDED,

    /** The platform's maximum obscuring opacity could not be read, so admission cannot be decided. */
    PASS_THROUGH_UNSUPPORTED,
}
