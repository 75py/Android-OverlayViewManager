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

/** Defines whether an overlay passes through, receives, or handles drag touches. */
public enum class OverlayTouchMode {
    /** The window is not touchable. The library does not change the View's touch listener. */
    PASS_THROUGH,

    /** The View receives touches. The library does not change the View's touch listener. */
    INTERACTIVE,

    /**
     * The library drags the window. It installs its own `View.OnTouchListener`, which replaces any
     * listener the host had set, because a View holds only one. Leaving this mode or disposing the
     * handle removes the listener without restoring the host's, so set it again if needed.
     */
    DRAGGABLE,
}
