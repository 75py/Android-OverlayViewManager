/*
 * Copyright 2026 75py
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.nagopy.android.overlayviewmanager

import android.os.Build
import android.view.View
import android.view.WindowManager
import com.nagopy.android.overlayviewmanager.internal.MaximumObscuringOpacity
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowManager
import com.nagopy.android.overlayviewmanager.internal.PassThroughOpacityRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings

/**
 * Covers the Android 12+ (API 31+) application-scope [OverlayTouchMode.PASS_THROUGH] opacity
 * admission described in the T06 contract: the conservative combined opacity
 * `1 - product(1 - alpha_i)` over every attached library-managed application pass-through handle
 * of this process plus the candidate, checked only for the opt-in
 * [CrossUidPassThrough.WHEN_SYSTEM_ALLOWS], on API 31+, against an injected
 * [com.nagopy.android.overlayviewmanager.internal.MaximumObscuringOpacitySource] so no shadow of
 * `InputManager` is required.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S], manifest = Config.NONE)
class PassThroughOpacityAdmissionTest {

    @Before fun setUp() {
        ShadowSettings.setCanDrawOverlays(true)
        PassThroughOpacityRegistry.resetForTests()
    }

    @After fun tearDown() {
        PassThroughOpacityRegistry.resetForTests()
    }

    @Test fun singleHandleBelowMaxIsAdmitted() {
        MaximumObscuringOpacity.setForTests { 0.8f }
        val result = handle(alpha = .7f).show()
        assertTrue(result.isSuccess)
        assertEquals(OverlayState.ATTACHED, result.state)
    }

    @Test fun singleHandleAtMaxIsAdmitted() {
        MaximumObscuringOpacity.setForTests { 0.8f }
        val result = handle(alpha = .8f).show()
        assertTrue(result.isSuccess)
    }

    @Test fun singleHandleAboveMaxIsRejectedWithoutAPlatformCallAndStateUnchanged() {
        MaximumObscuringOpacity.setForTests { 0.8f }
        val backend = RecordingBackend()
        val overlay = handle(alpha = .9f, backend = backend)

        val result = overlay.show()

        assertFalse(result.isSuccess)
        assertEquals(OverlayFailure.PASS_THROUGH_OPACITY_EXCEEDED, result.failure)
        assertNull(result.cause)
        assertEquals(OverlayState.CONFIGURED, overlay.state)
        assertEquals(0, backend.addCalls)
    }

    @Test fun combinedOpacityOfTwoHandlesExceedsMaxWhileEachIsIndividuallyBelow() {
        MaximumObscuringOpacity.setForTests { 0.8f }
        val first = handle(alpha = .6f)
        assertTrue(first.show().isSuccess) // .6 alone is below .8

        val second = handle(alpha = .6f)
        val result = second.show() // 1 - (.4 * .4) = .84 > .8

        assertFalse(result.isSuccess)
        assertEquals(OverlayFailure.PASS_THROUGH_OPACITY_EXCEEDED, result.failure)
        assertEquals(OverlayState.CONFIGURED, second.state)
    }

    @Test fun updateLoweringAlphaIsAdmittedAndTheRegistryReflectsTheLoweredValue() {
        MaximumObscuringOpacity.setForTests { 0.8f }
        val first = handle(alpha = .75f)
        assertTrue(first.show().isSuccess)

        val lowered = first.update(first.spec.copy(alpha = .5f))
        assertTrue(lowered.isSuccess)
        assertEquals(.5f, first.spec.alpha, 0f)

        // If the registry still held the pre-update .75, this would be 1-(.25*.5)=.875 > .8.
        // With the lowered .5 on record it is 1-(.5*.5)=.75 <= .8, so the second handle is admitted.
        val second = handle(alpha = .5f)
        assertTrue(second.show().isSuccess)
    }

    @Test fun hideFreesTheBudgetForAnotherHandle() {
        MaximumObscuringOpacity.setForTests { 0.8f }
        val first = handle(alpha = .6f)
        assertTrue(first.show().isSuccess)
        val second = handle(alpha = .6f)
        assertFalse(second.show().isSuccess)

        assertTrue(first.hide().isSuccess)
        assertTrue(second.show().isSuccess)
    }

    @Test fun disposeFreesTheBudgetForAnotherHandle() {
        MaximumObscuringOpacity.setForTests { 0.8f }
        val first = handle(alpha = .6f)
        assertTrue(first.show().isSuccess)
        val second = handle(alpha = .6f)
        assertFalse(second.show().isSuccess)

        assertTrue(first.dispose().isSuccess)
        assertTrue(second.show().isSuccess)
    }

    @Test fun unsupportedSourceFailsClosedWithoutAPlatformCall() {
        MaximumObscuringOpacity.setForTests { null }
        val backend = RecordingBackend()
        val overlay = handle(alpha = .1f, backend = backend)

        val result = overlay.show()

        assertFalse(result.isSuccess)
        assertEquals(OverlayFailure.PASS_THROUGH_UNSUPPORTED, result.failure)
        assertNull(result.cause)
        assertEquals(0, backend.addCalls)
    }

    @Test fun sourceThrowingIsTreatedAsUnsupported() {
        MaximumObscuringOpacity.setForTests { throw IllegalStateException("input service unavailable") }
        val result = handle(alpha = .1f).show()
        assertEquals(OverlayFailure.PASS_THROUGH_UNSUPPORTED, result.failure)
    }

    @Test
    @Config(sdk = [30], manifest = Config.NONE)
    fun api30AcceptsTheOptionWithoutQueryingOrCheckingOpacity() {
        MaximumObscuringOpacity.setForTests { throw AssertionError("must not query the platform below API 31") }
        val result = handle(alpha = 1f).show()
        assertTrue(result.isSuccess)
    }

    @Test fun sameUidOnlyIsNeverAdmissionChecked() {
        MaximumObscuringOpacity.setForTests { 0f } // would reject everything if ever consulted
        val overlay = OverlayView(
            View(RuntimeEnvironment.getApplication()),
            OverlayScope.APPLICATION,
            RecordingBackend(),
            OverlaySpec(alpha = 1f, touchMode = OverlayTouchMode.PASS_THROUGH, crossUidPassThrough = CrossUidPassThrough.SAME_UID_ONLY),
        )
        assertTrue(overlay.show().isSuccess)
    }

    @Test fun sameUidOnlyHandleCountsInTheCombinedSumEvenThoughItIsNeverItselfGated() {
        MaximumObscuringOpacity.setForTests { 0.8f }
        val sameUidOnly = OverlayView(
            View(RuntimeEnvironment.getApplication()),
            OverlayScope.APPLICATION,
            RecordingBackend(),
            OverlaySpec(alpha = 1f, touchMode = OverlayTouchMode.PASS_THROUGH, crossUidPassThrough = CrossUidPassThrough.SAME_UID_ONLY),
        )
        assertTrue(sameUidOnly.show().isSuccess) // never gated, so alpha = 1f is accepted

        // A WHEN_SYSTEM_ALLOWS candidate must still be judged against the combined sum that
        // includes the ungated SAME_UID_ONLY handle's alpha.
        val result = handle(alpha = .1f).show() // 1-((1-1)*(1-.1)) = 1 > .8
        assertFalse(result.isSuccess)
        assertEquals(OverlayFailure.PASS_THROUGH_OPACITY_EXCEEDED, result.failure)
    }

    @Test fun activityScopeIsNeverAdmissionChecked() {
        MaximumObscuringOpacity.setForTests { 0f } // would reject everything if ever consulted
        val overlay = OverlayView(
            View(RuntimeEnvironment.getApplication()),
            OverlayScope.ACTIVITY,
            RecordingBackend(),
            OverlaySpec(alpha = 1f, touchMode = OverlayTouchMode.PASS_THROUGH, crossUidPassThrough = CrossUidPassThrough.WHEN_SYSTEM_ALLOWS),
        )
        assertTrue(overlay.show().isSuccess)
    }

    @Test fun everyUpdateIncludingXyOnlyReQueriesTheOpacitySourceWithNoCaching() {
        var queries = 0
        MaximumObscuringOpacity.setForTests { queries++; 0.8f }
        val overlay = handle(alpha = .7f)
        assertTrue(overlay.show().isSuccess)
        assertEquals(1, queries)

        assertTrue(overlay.update(overlay.spec.copy(x = 42, y = 24)).isSuccess)
        assertEquals(2, queries)
    }

    @Test fun sourceMaxChangingAfterAttachIsReJudgedOnAnXOnlyUpdate() {
        var maximum = 0.9f
        MaximumObscuringOpacity.setForTests { maximum }
        val overlay = handle(alpha = .7f) // .7 <= .9, admitted
        assertTrue(overlay.show().isSuccess)

        // The platform maximum drops below the already-attached alpha; a later x-only update
        // (no alpha change at all) must be judged against the new maximum, not the one at attach.
        maximum = 0.5f
        val result = overlay.update(overlay.spec.copy(x = 10))

        assertFalse(result.isSuccess)
        assertEquals(OverlayFailure.PASS_THROUGH_OPACITY_EXCEEDED, result.failure)
        assertEquals(0, overlay.spec.x) // rejected: effective spec (including x) stays as attached
    }

    @Test fun disposeRemovesTheHandleFromTheRegistryEntirely() {
        MaximumObscuringOpacity.setForTests { 0.8f }
        val overlay = handle(alpha = .6f)
        assertTrue(overlay.show().isSuccess)
        assertTrue(PassThroughOpacityRegistry.containsForTests(overlay))

        assertTrue(overlay.dispose().isSuccess)

        assertFalse(PassThroughOpacityRegistry.containsForTests(overlay))
    }

    @Test fun failedHideLeavesTheRegistryEntryAndBudgetInPlace() {
        MaximumObscuringOpacity.setForTests { 0.8f }
        val backend = FailingHideBackend()
        val overlay = OverlayView(View(RuntimeEnvironment.getApplication()), OverlayScope.APPLICATION, backend, OverlaySpec(alpha = .6f, touchMode = OverlayTouchMode.PASS_THROUGH, crossUidPassThrough = CrossUidPassThrough.WHEN_SYSTEM_ALLOWS))
        assertTrue(overlay.show().isSuccess)
        assertTrue(PassThroughOpacityRegistry.containsForTests(overlay))

        backend.hideFailure = IllegalStateException("rejected")
        assertFalse(overlay.hide().isSuccess)
        assertEquals(OverlayState.ATTACHED, overlay.state)

        // The budget the failed hide would have freed must still be spent: a second handle whose
        // combined opacity only fits if the first one had actually been freed must still fail.
        val second = handle(alpha = .6f)
        assertFalse(second.show().isSuccess)
    }

    private fun handle(alpha: Float, backend: OverlayWindowManager = RecordingBackend()): OverlayView<View> = OverlayView(
        View(RuntimeEnvironment.getApplication()),
        OverlayScope.APPLICATION,
        backend,
        OverlaySpec(alpha = alpha, touchMode = OverlayTouchMode.PASS_THROUGH, crossUidPassThrough = CrossUidPassThrough.WHEN_SYSTEM_ALLOWS),
    )

    private class RecordingBackend : OverlayWindowManager() {
        var addCalls = 0
        override fun show(view: View, params: WindowManager.LayoutParams) { addCalls++ }
        override fun update(view: View, params: WindowManager.LayoutParams) = Unit
        override fun hide(view: View) = Unit
    }

    private class FailingHideBackend(var hideFailure: Throwable? = null) : OverlayWindowManager() {
        override fun show(view: View, params: WindowManager.LayoutParams) = Unit
        override fun update(view: View, params: WindowManager.LayoutParams) = Unit
        override fun hide(view: View) { hideFailure?.let { throw it } }
    }
}
