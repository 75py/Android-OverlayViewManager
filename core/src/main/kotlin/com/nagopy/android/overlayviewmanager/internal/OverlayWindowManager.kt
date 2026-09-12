/* Copyright 2026 75py. Licensed under the Apache License, Version 2.0. */
package com.nagopy.android.overlayviewmanager.internal

import android.app.Activity
import android.view.View
import android.view.WindowManager
import androidx.annotation.RestrictTo
import java.util.WeakHashMap

/** Synchronous WindowManager adapter. Platform exceptions intentionally reach the handle. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public open class OverlayWindowManager @JvmOverloads constructor(private var windowManager: WindowManager? = null) {
    public open fun show(view: View, params: WindowManager.LayoutParams) { requireWindowManager().addView(view, params) }
    public open fun update(view: View, params: WindowManager.LayoutParams) { requireWindowManager().updateViewLayout(view, params) }
    public open fun hide(view: View) { requireWindowManager().removeViewImmediate(view) }
    private fun requireWindowManager(): WindowManager = windowManager ?: error("OverlayWindowManager is not initialized.")

    public companion object {
        @Volatile private var applicationInstance: OverlayWindowManager = OverlayWindowManager()
        private val activityInstances = WeakHashMap<Activity, OverlayWindowManager>()
        @JvmStatic public fun getApplicationInstance(): OverlayWindowManager = applicationInstance
        @JvmStatic public fun initApplicationInstance(windowManager: WindowManager) { applicationInstance = OverlayWindowManager(windowManager) }
        @JvmStatic public fun getActivityInstance(activity: Activity): OverlayWindowManager = synchronized(activityInstances) { activityInstances.getOrPut(activity) { OverlayWindowManager(activity.windowManager) } }
        @JvmStatic @RestrictTo(RestrictTo.Scope.TESTS) public fun setApplicationInstance(instance: OverlayWindowManager) { applicationInstance = instance }
        @JvmStatic @RestrictTo(RestrictTo.Scope.TESTS) public fun setActivityInstance(activity: Activity, instance: OverlayWindowManager) { synchronized(activityInstances) { activityInstances[activity] = instance } }
    }
}
