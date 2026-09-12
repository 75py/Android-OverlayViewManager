/*
 * Copyright 2026 75py
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.nagopy.android.overlayviewmanager

import android.app.Application
import android.os.Build
import android.provider.Settings
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings
import java.util.concurrent.atomic.AtomicReference

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.VANILLA_ICE_CREAM], manifest = Config.NONE)
class OverlayPermissionTest {
    @After
    fun tearDown() {
        managerInstanceField().set(null, null)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.M], manifest = Config.NONE)
    fun isGranted_reflectsDeniedAndGrantedStateAtApi23() {
        val permission = permission()

        ShadowSettings.setCanDrawOverlays(false)
        assertFalse(permission.isGranted())
        ShadowSettings.setCanDrawOverlays(true)
        assertTrue(permission.isGranted())
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.O], manifest = Config.NONE)
    fun isGranted_reflectsDeniedAndGrantedStateAtApi26() {
        val permission = permission()

        ShadowSettings.setCanDrawOverlays(false)
        assertFalse(permission.isGranted())
        ShadowSettings.setCanDrawOverlays(true)
        assertTrue(permission.isGranted())
    }

    @Test
    fun isGranted_queriesTheCurrentModernPlatformStateFromWorkerThread() {
        val permission = permission()
        ShadowSettings.setCanDrawOverlays(false)
        val result = AtomicReference<Boolean>()
        val failure = AtomicReference<Throwable>()

        Thread {
            try {
                result.set(permission.isGranted())
            } catch (error: Throwable) {
                failure.set(error)
            }
        }.apply { start(); join() }

        assertEquals(null, failure.get())
        assertFalse(result.get())
        ShadowSettings.setCanDrawOverlays(true)
        assertTrue(permission.isGranted())
    }

    @Test
    fun settingsIntent_hasPackageActionAndDataWithoutReusingAnIntent() {
        val permission = permission()
        val first = permission.settingsIntent()
        val second = permission.settingsIntent()

        assertEquals(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, first.action)
        assertEquals("package:${RuntimeEnvironment.getApplication<Application>().packageName}", first.dataString)
        assertNotSame(first, second)
        assertEquals(first.action, second.action)
        assertEquals(first.data, second.data)
        assertEquals(0, first.flags)
    }

    @Test
    fun manager_returnsTheSameImmutablePermissionHelper() {
        val manager = initializedManager()

        assertSame(manager.overlayPermission(), manager.overlayPermission())
    }

    private fun permission(): OverlayPermission = initializedManager().overlayPermission()

    private fun initializedManager(): OverlayViewManager {
        OverlayViewManager.init(RuntimeEnvironment.getApplication())
        return OverlayViewManager.getInstance()
    }

    private fun managerInstanceField() = OverlayViewManager::class.java.getDeclaredField("instance").apply {
        isAccessible = true
    }
}
