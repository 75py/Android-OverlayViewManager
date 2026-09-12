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

package com.nagopy.android.overlayviewmanager;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.view.Gravity;
import android.view.ViewGroup;

import org.junit.Test;

public class OverlaySpecJavaConsumerTest {

    @Test
    public void defaultConstructorAndAccessors_areAvailableToJava() {
        OverlaySpec spec = new OverlaySpec();

        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, spec.getWidth());
        assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, spec.getHeight());
        assertEquals(Gravity.TOP | Gravity.START, spec.getGravity());
        assertEquals(OverlayTouchMode.PASS_THROUGH, spec.getTouchMode());
        assertEquals(CrossUidPassThrough.SAME_UID_ONLY, spec.getCrossUidPassThrough());
        assertFalse(spec.getAllowOutsideBounds());
        assertNull(spec.getScreenBrightness());
    }

    @Test
    public void builderAndToBuilder_preserveTheJavaSurface() {
        OverlaySpec spec = new OverlaySpec.Builder()
                .setWidth(12)
                .setHeight(ViewGroup.LayoutParams.MATCH_PARENT)
                .setGravity(Gravity.BOTTOM | Gravity.END)
                .setX(1)
                .setY(-2)
                .setHorizontalMargin(0.1f)
                .setVerticalMargin(0.2f)
                .setAlpha(0.3f)
                .setTouchMode(OverlayTouchMode.INTERACTIVE)
                .setCrossUidPassThrough(CrossUidPassThrough.WHEN_SYSTEM_ALLOWS)
                .setAllowOutsideBounds(true)
                .setScreenBrightness(Float.valueOf(0.4f))
                .build();

        assertEquals(12, spec.getWidth());
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, spec.getHeight());
        assertEquals(Gravity.BOTTOM | Gravity.END, spec.getGravity());
        assertEquals(1, spec.getX());
        assertEquals(-2, spec.getY());
        assertEquals(0.1f, spec.getHorizontalMargin(), 0f);
        assertEquals(0.2f, spec.getVerticalMargin(), 0f);
        assertEquals(0.3f, spec.getAlpha(), 0f);
        assertEquals(OverlayTouchMode.INTERACTIVE, spec.getTouchMode());
        assertEquals(CrossUidPassThrough.WHEN_SYSTEM_ALLOWS, spec.getCrossUidPassThrough());
        assertTrue(spec.getAllowOutsideBounds());
        assertEquals(Float.valueOf(0.4f), spec.getScreenBrightness());
        assertEquals(spec, spec.toBuilder().build());
    }

    @Test
    public void resultAccessorsAndIsSuccess_areAvailableToJava() {
        IllegalArgumentException cause = new IllegalArgumentException("denied");
        OverlayResult success = new OverlayResult(OverlayState.ATTACHED, true, null, null);
        OverlayResult failure = new OverlayResult(
                OverlayState.CONFIGURED,
                false,
                OverlayFailure.PERMISSION_DENIED,
                cause);

        assertTrue(success.isSuccess());
        assertFalse(failure.isSuccess());
        assertEquals(OverlayState.CONFIGURED, failure.getState());
        assertFalse(failure.getChanged());
        assertEquals(OverlayFailure.PERMISSION_DENIED, failure.getFailure());
        assertSame(cause, failure.getCause());
    }
}
