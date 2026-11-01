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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlaySpecKotlinConsumerTest {
    @Test
    fun defaultConfiguration_hasApprovedValues() {
        val spec = OverlaySpec()

        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, spec.width)
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, spec.height)
        assertEquals(Gravity.TOP or Gravity.START, spec.gravity)
        assertEquals(0, spec.x)
        assertEquals(0, spec.y)
        assertEquals(0f, spec.horizontalMargin, 0f)
        assertEquals(0f, spec.verticalMargin, 0f)
        assertEquals(1f, spec.alpha, 0f)
        assertEquals(OverlayTouchMode.PASS_THROUGH, spec.touchMode)
        assertEquals(CrossUidPassThrough.SAME_UID_ONLY, spec.crossUidPassThrough)
        assertFalse(spec.allowOutsideBounds)
        assertNull(spec.screenBrightness)
    }

    @Test
    fun namedArguments_acceptValidBoundaryValues() {
        val spec = OverlaySpec(
            width = ViewGroup.LayoutParams.MATCH_PARENT,
            height = 0,
            horizontalMargin = 1f,
            verticalMargin = 1f,
            alpha = 0f,
            screenBrightness = 1f,
        )

        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, spec.width)
        assertEquals(0, spec.height)
        assertEquals(1f, spec.horizontalMargin, 0f)
        assertEquals(1f, spec.verticalMargin, 0f)
        assertEquals(0f, spec.alpha, 0f)
        assertEquals(1f, spec.screenBrightness!!, 0f)
    }

    @Test
    fun directConstruction_rejectsInvalidNumericValuesAndDimensions() {
        assertThrows(IllegalArgumentException::class.java) { OverlaySpec(width = -3) }
        assertThrows(IllegalArgumentException::class.java) { OverlaySpec(height = -4) }
        assertThrows(IllegalArgumentException::class.java) { OverlaySpec(alpha = Float.NaN) }
        assertThrows(IllegalArgumentException::class.java) {
            OverlaySpec(horizontalMargin = Float.POSITIVE_INFINITY)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OverlaySpec(verticalMargin = -0.01f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OverlaySpec(screenBrightness = Float.NEGATIVE_INFINITY)
        }
    }

    @Test
    fun copy_revalidatesChangedValuesAndCreatesIndependentValue() {
        val original = OverlaySpec(x = 7, alpha = 0.5f)
        val copied = original.copy(x = 8)

        assertEquals(7, original.x)
        assertEquals(8, copied.x)
        assertThrows(IllegalArgumentException::class.java) {
            original.copy(alpha = 1.1f)
        }
    }

    @Test
    fun builder_createsIndependentSnapshotsAndRejectsInvalidSetterValues() {
        val builder = OverlaySpec.Builder().setX(10).setAlpha(0.25f)
        val first = builder.build()
        val second = builder.setX(20).setAlpha(0.75f).build()

        assertEquals(10, first.x)
        assertEquals(0.25f, first.alpha, 0f)
        assertEquals(20, second.x)
        assertEquals(0.75f, second.alpha, 0f)
        assertThrows(IllegalArgumentException::class.java) {
            OverlaySpec.Builder().setWidth(-3)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OverlaySpec.Builder().setHorizontalMargin(Float.NaN)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OverlaySpec.Builder().setVerticalMargin(1.01f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OverlaySpec.Builder().setAlpha(Float.NEGATIVE_INFINITY)
        }
        assertThrows(IllegalArgumentException::class.java) {
            OverlaySpec.Builder().setScreenBrightness(-0.01f)
        }
    }

    @Test
    fun toBuilder_preservesEveryPropertyAndNoOpValuesAreEqual() {
        val original = OverlaySpec(
            width = 24,
            height = ViewGroup.LayoutParams.MATCH_PARENT,
            gravity = Gravity.BOTTOM or Gravity.END,
            x = 3,
            y = -4,
            horizontalMargin = 0.2f,
            verticalMargin = 0.8f,
            alpha = 0.6f,
            touchMode = OverlayTouchMode.DRAGGABLE,
            crossUidPassThrough = CrossUidPassThrough.WHEN_SYSTEM_ALLOWS,
            allowOutsideBounds = true,
            screenBrightness = 0.4f,
        )

        val rebuilt = original.toBuilder().build()

        assertEquals(original, rebuilt)
        assertEquals(original.hashCode(), rebuilt.hashCode())
        assertEquals(original, original.copy())
    }

    @Test
    fun resultProjection_isDerivedFromFailureOnly() {
        val success = OverlayResult(OverlayState.ATTACHED, true, null, null)
        val diagnostic = IllegalStateException("rejected")
        val failure = OverlayResult(
            OverlayState.CONFIGURED,
            false,
            OverlayFailure.WINDOW_MANAGER_REJECTED,
            diagnostic,
        )

        assertTrue(success.isSuccess)
        assertFalse(failure.isSuccess)
        assertEquals(OverlayState.CONFIGURED, failure.state)
        assertFalse(failure.changed)
        assertEquals(OverlayFailure.WINDOW_MANAGER_REJECTED, failure.failure)
        assertEquals(diagnostic, failure.cause)
    }
}
