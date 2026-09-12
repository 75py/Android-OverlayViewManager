/* Copyright 2026 75py. Licensed under the Apache License, Version 2.0. */
package com.nagopy.android.overlayviewmanager.internal

import android.app.Activity
import androidx.annotation.MainThread
import androidx.annotation.RestrictTo
import com.nagopy.android.overlayviewmanager.OverlayView
import java.util.WeakHashMap

/**
 * Main-thread-only, process-wide registry of the live activity-scoped [OverlayView] handles of
 * every Activity, keyed weakly by [Activity]. A handle registers here at factory creation and
 * deregisters on `dispose()`/forced disposal (see `OverlayView.disposeForActivityDestruction`);
 * the registry's values reach the Activity only through those still-live handles, never through
 * the weak key alone, so an Activity's entry is removed explicitly by [disposeAll] rather than
 * left to key collection.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
internal object ActivityOverlayRegistry {
    private val handlesByActivity = WeakHashMap<Activity, MutableSet<OverlayView<*>>>()

    @MainThread
    internal fun register(activity: Activity, handle: OverlayView<*>) {
        handlesByActivity.getOrPut(activity) { LinkedHashSet() }.add(handle)
    }

    @MainThread
    internal fun deregister(activity: Activity, handle: OverlayView<*>) {
        val handles = handlesByActivity[activity] ?: return
        handles.remove(handle)
        if (handles.isEmpty()) handlesByActivity.remove(activity)
    }

    /** Forces every live handle of [activity] to release, then removes its registry entry. */
    @MainThread
    internal fun disposeAll(activity: Activity) {
        val handles = handlesByActivity.remove(activity) ?: return
        for (handle in ArrayList(handles)) handle.disposeForActivityDestruction()
    }
}
