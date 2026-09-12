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
    PERMISSION_DENIED,
    ACTIVITY_DESTROYED,
    INVALID_WINDOW_TOKEN,
    WINDOW_MANAGER_REJECTED,
    ALREADY_HAS_PARENT,
    NOT_ATTACHED,
    PASS_THROUGH_OPACITY_EXCEEDED,
    PASS_THROUGH_UNSUPPORTED,
}
