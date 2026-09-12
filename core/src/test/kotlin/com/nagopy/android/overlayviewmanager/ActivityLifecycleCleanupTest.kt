/*
 * Copyright 2026 75py
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.nagopy.android.overlayviewmanager

import android.app.Activity
import android.os.Build
import android.view.View
import android.view.WindowManager
import com.nagopy.android.overlayviewmanager.internal.ActivityOverlayRegistry
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowManager
import com.nagopy.android.overlayviewmanager.internal.PassThroughOpacityRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings
import java.lang.reflect.Field

/**
 * Covers the T08 Activity destruction terminal path (`OverlayViewManager.onActivityDestroyed` via
 * a single process-wide `ActivityLifecycleCallbacks`) and the reference-release audit of
 * `OverlayWindowManager.activityInstances`. Acceptance is the deterministic structural assertions
 * below, per the FROZEN T08 CONTRACT F3 -- registry/instance-map membership, cleared handle
 * references, and the classified diagnostic -- rather than a GC/WeakReference observation, which
 * this suite intentionally omits: Robolectric's own `ActivityController`/shadow activity thread
 * retains additional bookkeeping references to the Activity that this library does not own or
 * control, so a GC-based pass/fail signal would reflect Robolectric's internals rather than this
 * library's cleanup correctness (documented as a known limitation in PR_BODY_T08.md).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P], manifest = Config.NONE)
class ActivityLifecycleCleanupTest {

    @Before fun setUp() {
        ShadowSettings.setCanDrawOverlays(true)
        clearActivityOverlayRegistry()
        PassThroughOpacityRegistry.resetForTests()
    }

    @After fun tearDown() {
        managerInstanceField().set(null, null)
        clearActivityOverlayRegistry()
        PassThroughOpacityRegistry.resetForTests()
    }

    @Test fun forcedDisposalOfAttachedHandleRemovesOnceAndClearsEveryReference() {
        val application = RuntimeEnvironment.getApplication()
        OverlayViewManager.init(application)
        val controller = Robolectric.buildActivity(Activity::class.java).create().start().resume()
        val activity = controller.get()
        val handle = OverlayViewManager.getInstance().newOverlayView(View(activity), activity)

        assertTrue(handle.show().isSuccess)
        assertEquals(OverlayState.ATTACHED, handle.state)
        assertTrue(activityOverlayRegistryContains(activity))
        assertTrue(activityInstancesContains(activity))

        controller.pause().stop().destroy()

        assertEquals(OverlayState.DISPOSED, handle.state)
        assertNull(handle.lastFailure)
        assertFalse(activityOverlayRegistryContains(activity))
        assertFalse(activityInstancesContains(activity))
        assertNull(privateField(handle, "ownedView"))
        assertNull(privateField(handle, "backend"))
        assertNull(privateField(handle, "detachListener"))
        assertNull(privateField(handle, "onActivityRelease"))
        assertThrows(IllegalStateException::class.java) { handle.view }
    }

    @Test fun forcedDisposalOfConfiguredHandleDisposesWithoutABackendCall() {
        val application = RuntimeEnvironment.getApplication()
        OverlayViewManager.init(application)
        val controller = Robolectric.buildActivity(Activity::class.java).create().start().resume()
        val activity = controller.get()
        val handle = OverlayViewManager.getInstance().newOverlayView(View(activity), activity)
        assertEquals(OverlayState.CONFIGURED, handle.state)

        controller.pause().stop().destroy()

        assertEquals(OverlayState.DISPOSED, handle.state)
        assertNull(handle.lastFailure)
        assertFalse(activityOverlayRegistryContains(activity))
    }

    @Test fun forcedDisposalClassifiesAFailedRemovalAndDoesNotRetainTheException() {
        val application = RuntimeEnvironment.getApplication()
        OverlayViewManager.init(application)
        val controller = Robolectric.buildActivity(Activity::class.java).create().start().resume()
        val activity = controller.get()
        OverlayWindowManager.setActivityInstance(activity, RemoveThrowingBackend(WindowManager.BadTokenException("bad token")))
        val handle = OverlayViewManager.getInstance().newOverlayView(View(activity), activity)
        assertTrue(handle.show().isSuccess)

        controller.pause().stop().destroy()

        assertEquals(OverlayState.DISPOSED, handle.state)
        assertEquals(OverlayFailure.INVALID_WINDOW_TOKEN, handle.lastFailure)
    }

    @Test fun afterForcedDisposalShowAndRepeatedDisposePreserveTheDiagnostic() {
        val application = RuntimeEnvironment.getApplication()
        OverlayViewManager.init(application)
        val controller = Robolectric.buildActivity(Activity::class.java).create().start().resume()
        val activity = controller.get()
        OverlayWindowManager.setActivityInstance(activity, RemoveThrowingBackend(IllegalStateException("busy")))
        val handle = OverlayViewManager.getInstance().newOverlayView(View(activity), activity)
        handle.show()

        controller.pause().stop().destroy()

        assertEquals(OverlayFailure.WINDOW_MANAGER_REJECTED, handle.lastFailure)

        val showAfterDisposal = handle.show()
        assertFalse(showAfterDisposal.isSuccess)
        assertEquals(OverlayState.DISPOSED, handle.state)
        assertEquals(OverlayFailure.WINDOW_MANAGER_REJECTED, handle.lastFailure)

        val updateAfterDisposal = handle.update(handle.spec)
        assertFalse(updateAfterDisposal.isSuccess)

        val hideAfterDisposal = handle.hide()
        assertTrue(hideAfterDisposal.isSuccess)
        assertFalse(hideAfterDisposal.changed)

        val repeatedDispose = handle.dispose()
        assertTrue(repeatedDispose.isSuccess)
        assertFalse(repeatedDispose.changed)
        assertEquals(OverlayFailure.WINDOW_MANAGER_REJECTED, handle.lastFailure)
    }

    @Test fun everyLiveHandleOfTheDestroyedActivityIsDisposed() {
        val application = RuntimeEnvironment.getApplication()
        OverlayViewManager.init(application)
        val controller = Robolectric.buildActivity(Activity::class.java).create().start().resume()
        val activity = controller.get()
        val manager = OverlayViewManager.getInstance()
        val attached = manager.newOverlayView(View(activity), activity)
        val configured = manager.newOverlayView(View(activity), activity)
        assertTrue(attached.show().isSuccess)

        controller.pause().stop().destroy()

        assertEquals(OverlayState.DISPOSED, attached.state)
        assertEquals(OverlayState.DISPOSED, configured.state)
    }

    @Test fun applicationScopedHandleIsUnaffectedByActivityDestruction() {
        val application = RuntimeEnvironment.getApplication()
        OverlayViewManager.init(application)
        val controller = Robolectric.buildActivity(Activity::class.java).create().start().resume()
        val activity = controller.get()
        val manager = OverlayViewManager.getInstance()
        val appHandle = manager.newOverlayView(View(application))
        assertTrue(appHandle.show().isSuccess)

        controller.pause().stop().destroy()

        assertEquals(OverlayState.ATTACHED, appHandle.state)
        assertTrue(appHandle.dispose().isSuccess)
    }

    @Test fun activityScopedHandleIsNeverInThePassThroughOpacityRegistry() {
        val application = RuntimeEnvironment.getApplication()
        OverlayViewManager.init(application)
        val controller = Robolectric.buildActivity(Activity::class.java).create().start().resume()
        val activity = controller.get()
        val handle = OverlayViewManager.getInstance().newOverlayView(
            View(activity),
            activity,
            OverlaySpec(touchMode = OverlayTouchMode.PASS_THROUGH),
        )
        assertFalse(PassThroughOpacityRegistry.containsForTests(handle))

        assertTrue(handle.show().isSuccess)
        assertFalse(PassThroughOpacityRegistry.containsForTests(handle))

        controller.pause().stop().destroy()

        assertFalse(PassThroughOpacityRegistry.containsForTests(handle))
    }

    private fun privateField(instance: Any, name: String): Any? = instance.javaClass.getDeclaredField(name).let {
        it.isAccessible = true
        it.get(instance)
    }

    private fun managerInstanceField(): Field = OverlayViewManager::class.java.getDeclaredField("instance").also { it.isAccessible = true }

    /** Reflects into `ActivityOverlayRegistry`'s private `handlesByActivity` map (no test accessor exists). */
    private fun activityOverlayRegistryMap(): MutableMap<Activity, *> {
        val field = ActivityOverlayRegistry::class.java.getDeclaredField("handlesByActivity")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return field.get(ActivityOverlayRegistry) as MutableMap<Activity, *>
    }

    private fun activityOverlayRegistryContains(activity: Activity): Boolean = activityOverlayRegistryMap().containsKey(activity)

    private fun clearActivityOverlayRegistry() = activityOverlayRegistryMap().clear()

    /** Reflects into `OverlayWindowManager.Companion`'s private `activityInstances` map (no test accessor exists). */
    private fun activityInstancesContains(activity: Activity): Boolean {
        val field = OverlayWindowManager.Companion::class.java.getDeclaredField("activityInstances")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val map = field.get(OverlayWindowManager.Companion) as Map<Activity, *>
        return map.containsKey(activity)
    }

    private class RemoveThrowingBackend(private val failure: Throwable) : OverlayWindowManager() {
        override fun show(view: View, params: WindowManager.LayoutParams) = Unit
        override fun update(view: View, params: WindowManager.LayoutParams) = Unit
        override fun hide(view: View) { throw failure }
    }
}
