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
 * The outcome of one overlay operation.
 *
 * [cause] is diagnostic information for this result only. Callers should use [failure] to
 * decide how to recover instead of relying on exception messages.
 */
public class OverlayResult(
    public val state: OverlayState,
    public val changed: Boolean,
    public val failure: OverlayFailure?,
    public val cause: Throwable?,
) {
    /** Returns whether this operation has no classified failure. */
    public val isSuccess: Boolean
        get() = failure == null
}
