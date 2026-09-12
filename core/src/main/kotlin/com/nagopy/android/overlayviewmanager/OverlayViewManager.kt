/*
 * Copyright 2026 75py
 * Licensed under the Apache License, Version 2.0 (the "License");
 */
package com.nagopy.android.overlayviewmanager

import android.app.Activity
import android.app.Application
import android.annotation.TargetApi
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Looper
import android.view.View
import android.view.WindowManager
import androidx.annotation.MainThread
import androidx.annotation.StringRes
import androidx.fragment.app.FragmentManager
import com.nagopy.android.overlayviewmanager.internal.ActivityOverlayRegistry
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowManager
import com.nagopy.android.overlayviewmanager.internal.SimpleActivityLifecycleCallbacks

/** Process-scoped factory for synchronous overlay handles. */
public class OverlayViewManager private constructor(private val application: Application) {
    private val windowManager: WindowManager = application.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val permission: OverlayPermission = OverlayPermission()

    init {
        OverlayWindowManager.initApplicationInstance(windowManager)
    }

    /** Creates an application-scoped overlay with default immutable configuration. */
    @MainThread public fun <T : View> newOverlayView(view: T): OverlayView<T> = newApplicationOverlay(view, OverlaySpec())
    /** Creates an application-scoped overlay with [spec]. */
    @MainThread public fun <T : View> newOverlayView(view: T, spec: OverlaySpec): OverlayView<T> = newApplicationOverlay(view, spec)
    /** Creates an activity-scoped overlay with default immutable configuration. */
    @MainThread public fun <T : View> newOverlayView(view: T, activity: Activity): OverlayView<T> = newActivityOverlay(view, activity, OverlaySpec())
    /** Creates an activity-scoped overlay with [spec]. */
    @MainThread public fun <T : View> newOverlayView(view: T, activity: Activity, spec: OverlaySpec): OverlayView<T> = newActivityOverlay(view, activity, spec)

    /** Returns the immutable overlay permission helper. See [OverlayPermission]. */
    public fun overlayPermission(): OverlayPermission = permission

    /** Temporary 2.x bridge. New code should use [OverlayPermission.isGranted] instead. */
    @Deprecated("Use OverlayPermission.isGranted from the host application instead.") public fun canDrawOverlays(): Boolean = overlayPermission().isGranted(application)

    /**
     * Temporary 2.x bridge retained until sample and consumer migration.
     * New code should use [OverlayPermission.settingsIntent] and own its rationale UI instead.
     */
    @Deprecated("Use OverlayPermission.settingsIntent from the host application instead.")
    @TargetApi(Build.VERSION_CODES.M)
    public fun showPermissionRequestDialog(fragmentManager: FragmentManager, @StringRes appNameId: Int) {
        PermissionRequestDialogFragment.newInstance(appNameId).show(fragmentManager, "PermissionRequestDialogFragment")
    }

    /** Temporary 2.x bridge retained until sample and consumer migration. */
    @Deprecated("Use OverlayPermission.settingsIntent from the host application instead.")
    @TargetApi(Build.VERSION_CODES.M)
    public fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !canDrawOverlays()) {
            val intent = overlayPermission().settingsIntent(application)
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            application.startActivity(intent)
        }
    }

    /** Temporary 2.x bridge retained until T05/T10 migrate geometry consumers. */
    @Deprecated("Obtain bounds and insets from the target window instead.")
    public fun getDisplayWidth(): Int = android.util.DisplayMetrics().let { metrics -> windowManager.defaultDisplay.getMetrics(metrics); metrics.widthPixels }

    /** Temporary 2.x bridge retained until T05/T10 migrate geometry consumers. */
    @Deprecated("Obtain bounds and insets from the target window instead.")
    public fun getDisplayHeight(): Int = android.util.DisplayMetrics().let { metrics -> windowManager.defaultDisplay.getMetrics(metrics); metrics.heightPixels }

    private fun <T : View> newApplicationOverlay(view: T, spec: OverlaySpec): OverlayView<T> {
        requireMainThread()
        require(view.parent == null) { "The managed view must not already have a parent." }
        require(spec.screenBrightness == null) { "screenBrightness is supported only for activity overlays." }
        return OverlayView(view, OverlayScope.APPLICATION, OverlayWindowManager.getApplicationInstance(), spec, permission)
    }

    private fun <T : View> newActivityOverlay(view: T, activity: Activity, spec: OverlaySpec): OverlayView<T> {
        requireMainThread()
        require(view.parent == null) { "The managed view must not already have a parent." }
        require(!activity.isFinishing && (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1 || !activity.isDestroyed)) { "Activity is finishing or destroyed." }
        lateinit var handle: OverlayView<T>
        handle = OverlayView(
            view,
            OverlayScope.ACTIVITY,
            OverlayWindowManager.getActivityInstance(activity),
            spec,
            permission,
        ) { ActivityOverlayRegistry.deregister(activity, handle) }
        ActivityOverlayRegistry.register(activity, handle)
        return handle
    }

    private fun requireMainThread() {
        val main = Looper.getMainLooper()
        check(main != null && Looper.myLooper() === main) { "OverlayViewManager must be used on the main thread." }
    }

    public companion object {
        @Volatile private var instance: OverlayViewManager? = null

        /**
         * Registered on the first successful [init], never again on a same-Application no-op init.
         * Forces every live activity-scoped handle of the destroyed Activity to release and drops
         * the Activity's entry from [OverlayWindowManager]'s per-Activity instance cache.
         */
        private val activityDestructionCallbacks: Application.ActivityLifecycleCallbacks =
            object : SimpleActivityLifecycleCallbacks() {
                override fun onActivityDestroyed(activity: Activity) {
                    ActivityOverlayRegistry.disposeAll(activity)
                    OverlayWindowManager.removeActivityInstance(activity)
                }
            }

        /** Initializes the singleton. Repeating this with the same Application is harmless. */
        @JvmStatic @MainThread public fun init(application: Application) {
            val main = Looper.getMainLooper()
            check(main != null && Looper.myLooper() === main) { "OverlayViewManager must be initialized on the main thread." }
            synchronized(this) {
                val existing = instance
                check(existing == null || existing.application === application) { "OverlayViewManager is already initialized for another Application." }
                if (existing == null) {
                    instance = OverlayViewManager(application)
                    application.registerActivityLifecycleCallbacks(activityDestructionCallbacks)
                }
            }
        }

        /** Returns the initialized process singleton. */
        @JvmStatic @MainThread public fun getInstance(): OverlayViewManager {
            val main = Looper.getMainLooper()
            check(main != null && Looper.myLooper() === main) { "OverlayViewManager must be accessed on the main thread." }
            return instance ?: throw IllegalStateException("OverlayViewManager is not initialized. Call init(Application) first.")
        }

    }
}
