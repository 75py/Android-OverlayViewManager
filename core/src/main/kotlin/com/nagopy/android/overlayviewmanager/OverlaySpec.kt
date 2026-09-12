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

import android.view.Gravity
import android.view.ViewGroup

/**
 * Immutable, value-based configuration for an overlay.
 *
 * Scope-specific rules are applied by the overlay factory. This value only validates its own
 * dimensions and normalized numeric properties.
 */
public data class OverlaySpec @JvmOverloads constructor(
    public val width: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    public val height: Int = ViewGroup.LayoutParams.WRAP_CONTENT,
    public val gravity: Int = Gravity.TOP or Gravity.START,
    public val x: Int = 0,
    public val y: Int = 0,
    public val horizontalMargin: Float = 0f,
    public val verticalMargin: Float = 0f,
    public val alpha: Float = 1f,
    public val touchMode: OverlayTouchMode = OverlayTouchMode.PASS_THROUGH,
    public val crossUidPassThrough: CrossUidPassThrough = CrossUidPassThrough.SAME_UID_ONLY,
    public val allowOutsideBounds: Boolean = false,
    public val screenBrightness: Float? = null,
) {
    init {
        validate()
    }

    /** Returns a Java-friendly mutable builder initialized from this value. */
    public fun toBuilder(): Builder = Builder(this)

    /** Java-friendly builder for [OverlaySpec]. */
    public class Builder {
        private var width: Int = ViewGroup.LayoutParams.WRAP_CONTENT
        private var height: Int = ViewGroup.LayoutParams.WRAP_CONTENT
        private var gravity: Int = Gravity.TOP or Gravity.START
        private var x: Int = 0
        private var y: Int = 0
        private var horizontalMargin: Float = 0f
        private var verticalMargin: Float = 0f
        private var alpha: Float = 1f
        private var touchMode: OverlayTouchMode = OverlayTouchMode.PASS_THROUGH
        private var crossUidPassThrough: CrossUidPassThrough = CrossUidPassThrough.SAME_UID_ONLY
        private var allowOutsideBounds: Boolean = false
        private var screenBrightness: Float? = null

        /** Creates a builder with the default configuration. */
        public constructor()

        /** Creates a builder initialized from [source]. */
        public constructor(source: OverlaySpec) {
            width = source.width
            height = source.height
            gravity = source.gravity
            x = source.x
            y = source.y
            horizontalMargin = source.horizontalMargin
            verticalMargin = source.verticalMargin
            alpha = source.alpha
            touchMode = source.touchMode
            crossUidPassThrough = source.crossUidPassThrough
            allowOutsideBounds = source.allowOutsideBounds
            screenBrightness = source.screenBrightness
        }

        /** Sets the requested window width. */
        public fun setWidth(width: Int): Builder = apply {
            validateDimension(width, "width")
            this.width = width
        }

        /** Sets the requested window height. */
        public fun setHeight(height: Int): Builder = apply {
            validateDimension(height, "height")
            this.height = height
        }

        /** Sets the requested gravity. */
        public fun setGravity(gravity: Int): Builder = apply {
            this.gravity = gravity
        }

        /** Sets the horizontal offset. */
        public fun setX(x: Int): Builder = apply {
            this.x = x
        }

        /** Sets the vertical offset. */
        public fun setY(y: Int): Builder = apply {
            this.y = y
        }

        /** Sets the horizontal normalized margin. */
        public fun setHorizontalMargin(margin: Float): Builder = apply {
            validateUnitInterval(margin, "horizontalMargin")
            horizontalMargin = margin
        }

        /** Sets the vertical normalized margin. */
        public fun setVerticalMargin(margin: Float): Builder = apply {
            validateUnitInterval(margin, "verticalMargin")
            verticalMargin = margin
        }

        /** Sets the normalized overlay alpha. */
        public fun setAlpha(alpha: Float): Builder = apply {
            validateUnitInterval(alpha, "alpha")
            this.alpha = alpha
        }

        /** Sets the overlay touch mode. */
        public fun setTouchMode(touchMode: OverlayTouchMode): Builder = apply {
            this.touchMode = touchMode
        }

        /** Sets the cross-UID pass-through policy. */
        public fun setCrossUidPassThrough(mode: CrossUidPassThrough): Builder = apply {
            crossUidPassThrough = mode
        }

        /** Sets whether normal layout bounds may be exceeded. */
        public fun setAllowOutsideBounds(allowOutsideBounds: Boolean): Builder = apply {
            this.allowOutsideBounds = allowOutsideBounds
        }

        /** Sets the optional normalized screen-brightness override. */
        public fun setScreenBrightness(brightness: Float?): Builder = apply {
            brightness?.let { validateUnitInterval(it, "screenBrightness") }
            screenBrightness = brightness
        }

        /** Builds an immutable configuration after validating every stored property. */
        public fun build(): OverlaySpec = OverlaySpec(
            width = width,
            height = height,
            gravity = gravity,
            x = x,
            y = y,
            horizontalMargin = horizontalMargin,
            verticalMargin = verticalMargin,
            alpha = alpha,
            touchMode = touchMode,
            crossUidPassThrough = crossUidPassThrough,
            allowOutsideBounds = allowOutsideBounds,
            screenBrightness = screenBrightness,
        )
    }

    private fun validate() {
        validateDimension(width, "width")
        validateDimension(height, "height")
        validateUnitInterval(horizontalMargin, "horizontalMargin")
        validateUnitInterval(verticalMargin, "verticalMargin")
        validateUnitInterval(alpha, "alpha")
        screenBrightness?.let { validateUnitInterval(it, "screenBrightness") }
    }

    private companion object {
        private fun validateDimension(value: Int, name: String) {
            require(
                value >= 0 ||
                    value == ViewGroup.LayoutParams.MATCH_PARENT ||
                    value == ViewGroup.LayoutParams.WRAP_CONTENT,
            ) { "$name must be non-negative, MATCH_PARENT, or WRAP_CONTENT." }
        }

        private fun validateUnitInterval(value: Float, name: String) {
            require(!value.isNaN() && !value.isInfinite() && value in 0f..1f) {
                "$name must be finite and in the range 0..1."
            }
        }
    }
}
