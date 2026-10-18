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

import android.os.Build
import android.os.Looper
import android.view.View
import android.view.WindowManager
import android.view.Gravity
import androidx.annotation.MainThread
import com.nagopy.android.overlayviewmanager.internal.Logger
import com.nagopy.android.overlayviewmanager.internal.MaximumObscuringOpacity
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowManager
import com.nagopy.android.overlayviewmanager.internal.PassThroughOpacityRegistry

/** A synchronous, main-thread-owned overlay handle. */
public class OverlayView<T : View> internal constructor(
    view: T,
    private val scope: OverlayScope,
    backend: OverlayWindowManager,
    initialSpec: OverlaySpec,
    private val permission: OverlayPermission = OverlayPermission(),
    activityReleaseCallback: (() -> Unit)? = null,
) {
    private var backend: OverlayWindowManager? = backend
    private var ownedView: T? = view
    private var detachListener: View.OnAttachStateChangeListener? = null
    /**
     * Invoked exactly once, from [release], to deregister this handle from the owning Activity's
     * entry in `ActivityOverlayRegistry`. Cleared immediately after invocation so that the closure
     * -- and whatever Activity it captured -- is not retained by a disposed handle.
     */
    private var onActivityRelease: (() -> Unit)? = activityReleaseCallback
    private var effectiveSpec: OverlaySpec = initialSpec
    private var effectiveDragListener: DraggableOnTouchListener<T>? = null
    @Volatile private var currentState: OverlayState = OverlayState.CONFIGURED
    @Volatile private var currentFailure: OverlayFailure? = null
    private var libraryRemovalInProgress: Boolean = false

    init {
        if (initialSpec.touchMode == OverlayTouchMode.DRAGGABLE) {
            effectiveDragListener = DraggableOnTouchListener(this)
        }
        applyTouchListener(effectiveDragListener)
    }

    /** The managed view. It cannot be read once this handle is disposed. */
    @get:MainThread
    public val view: T
        get() {
            requireMainThread()
            return ownedView ?: throw IllegalStateException("OverlayView is disposed.")
        }

    /** The last state established by this handle. This value is safe to read from any thread. */
    public val state: OverlayState
        get() = currentState

    /** The last classified failure. This value is safe to read from any thread. */
    public val lastFailure: OverlayFailure?
        get() = currentFailure

    /** The immutable effective configuration, readable on the main thread even after disposal. */
    @get:MainThread
    public val spec: OverlaySpec
        get() {
            requireMainThread()
            return effectiveSpec
        }

    /** Registers the view synchronously when it is not already attached. */
    @MainThread
    public fun show(): OverlayResult {
        requireMainThread()
        when (currentState) {
            OverlayState.DISPOSED -> return disposedFailure()
            OverlayState.ATTACHED -> return success(changed = false)
            OverlayState.CONFIGURED -> Unit
        }
        val candidate = effectiveSpec
        val managedView = ownedView ?: return failure(OverlayFailure.WINDOW_MANAGER_REJECTED, null)
        if (managedView.parent != null) return failure(OverlayFailure.ALREADY_HAS_PARENT, null)
        if (scope == OverlayScope.APPLICATION && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !permission.isGranted(managedView.context)) {
            return failure(OverlayFailure.PERMISSION_DENIED, null)
        }
        admitPassThroughOpacity(candidate, managedView)?.let { return failure(it, null) }
        try {
            backendOrThrow().show(managedView, layoutParams(candidate))
        } catch (exception: Exception) {
            return failure(classify(exception), exception)
        }
        updatePassThroughOpacityRegistration(candidate)
        accept(candidate, nextDragListener(candidate))
        currentState = OverlayState.ATTACHED
        installDetachListener(managedView)
        return success(changed = true)
    }

    /**
     * Applies [spec] synchronously or replaces pending configuration before first show.
     *
     * Unlike [show], this performs no application-scope permission preflight -- neither does
     * [hide] or [dispose]. Only the initial [show] checks [OverlayPermission.isGranted]. If the
     * overlay permission is revoked while [ATTACHED][OverlayState.ATTACHED], the platform reports
     * that on the next window operation: the backend throws [SecurityException], which is
     * classified [OverlayFailure.PERMISSION_DENIED] like any other backend rejection, while the
     * current [state] and the last effective [spec] are preserved unchanged. This mirrors how
     * every other platform rejection here is a later observation rather than a proactive check,
     * and avoids a redundant permission query on every mutation of an already-attached window.
     */
    @MainThread
    public fun update(spec: OverlaySpec): OverlayResult {
        requireMainThread()
        requireApplicationBrightnessIsAbsent(spec)
        if (currentState == OverlayState.DISPOSED) return disposedFailure()
        val nextDragListener = nextDragListener(spec)
        if (currentState == OverlayState.CONFIGURED) {
            if (spec == effectiveSpec) return success(changed = false)
            accept(spec, nextDragListener)
            return success(changed = true)
        }
        if (spec == effectiveSpec) return success(changed = false)
        val managedView = ownedView ?: return failure(OverlayFailure.WINDOW_MANAGER_REJECTED, null)
        admitPassThroughOpacity(spec, managedView)?.let { return failure(it, null) }
        try {
            backendOrThrow().update(managedView, layoutParams(spec))
        } catch (exception: Exception) {
            if (isNotAttached(exception)) {
                reconcileExternalDetach()
                return OverlayResult(currentState, false, OverlayFailure.NOT_ATTACHED, exception)
            }
            return failure(classify(exception), exception)
        }
        updatePassThroughOpacityRegistration(spec)
        accept(spec, nextDragListener)
        return success(changed = true)
    }

    /** Removes the view synchronously. A not-attached result is a successful reconciliation. */
    @MainThread
    public fun hide(): OverlayResult {
        requireMainThread()
        if (currentState == OverlayState.DISPOSED) return success(changed = false, preserveDiagnostic = true)
        if (currentState == OverlayState.CONFIGURED) return success(changed = false, preserveDiagnostic = true)
        val managedView = ownedView ?: return failure(OverlayFailure.WINDOW_MANAGER_REJECTED, null)
        libraryRemovalInProgress = true
        try {
            backendOrThrow().hide(managedView)
        } catch (exception: Exception) {
            libraryRemovalInProgress = false
            if (isNotAttached(exception)) {
                reconcileExternalDetach()
                return successfulReconciliation()
            }
            return failure(classify(exception), exception)
        }
        libraryRemovalInProgress = false
        currentState = OverlayState.CONFIGURED
        removeDetachListener(managedView)
        PassThroughOpacityRegistry.unregister(this)
        return success(changed = true)
    }

    /** Releases owned resources after a successful synchronous removal. */
    @MainThread
    public fun dispose(): OverlayResult {
        requireMainThread()
        if (currentState == OverlayState.DISPOSED) return success(changed = false, preserveDiagnostic = true)
        if (currentState == OverlayState.ATTACHED) {
            val managedView = ownedView ?: return failure(OverlayFailure.WINDOW_MANAGER_REJECTED, null)
            libraryRemovalInProgress = true
            try {
                backendOrThrow().hide(managedView)
            } catch (exception: Exception) {
                libraryRemovalInProgress = false
                if (isNotAttached(exception)) {
                    reconcileExternalDetach()
                    release(managedView)
                    return successfulReconciliation(OverlayState.DISPOSED)
                }
                return failure(classify(exception), exception)
            }
            libraryRemovalInProgress = false
            release(managedView)
            return success(changed = true)
        }
        ownedView?.let(::release)
        return success(changed = true, preserveDiagnostic = true)
    }

    /**
     * Forced cleanup for the owning Activity's `onActivityDestroyed`. If [ATTACHED][OverlayState.ATTACHED],
     * attempts exactly one `removeViewImmediate`; regardless of that outcome, releases every owned
     * reference and transitions to [DISPOSED][OverlayState.DISPOSED]. A failed removal is classified
     * with the existing [classify] mapping and retained as [lastFailure] -- the exception itself is
     * never retained. `DISPOSED` here means library ownership is released, not proof that the
     * framework removal succeeded. Idempotent: a no-op once already [DISPOSED][OverlayState.DISPOSED].
     */
    @MainThread
    internal fun disposeForActivityDestruction() {
        requireMainThread()
        if (currentState == OverlayState.DISPOSED) return
        val managedView = ownedView
        if (currentState == OverlayState.ATTACHED && managedView != null) {
            libraryRemovalInProgress = true
            try {
                backendOrThrow().hide(managedView)
            } catch (exception: Exception) {
                currentFailure = classify(exception)
                Logger.w(exception, "Forced Activity-destruction removeViewImmediate failed; classified as %s", currentFailure)
            } finally {
                libraryRemovalInProgress = false
            }
        }
        managedView?.let(::release)
    }

    private fun installDetachListener(managedView: T) {
        val listener = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) = Unit
            override fun onViewDetachedFromWindow(v: View) {
                if (!libraryRemovalInProgress && currentState == OverlayState.ATTACHED) reconcileExternalDetach()
            }
        }
        detachListener = listener
        managedView.addOnAttachStateChangeListener(listener)
    }

    private fun removeDetachListener(managedView: T) {
        detachListener?.let(managedView::removeOnAttachStateChangeListener)
        detachListener = null
    }

    private fun reconcileExternalDetach() {
        currentState = OverlayState.CONFIGURED
        currentFailure = OverlayFailure.NOT_ATTACHED
        ownedView?.let(::removeDetachListener)
        PassThroughOpacityRegistry.unregister(this)
    }

    private fun release(managedView: T) {
        removeDetachListener(managedView)
        managedView.setOnTouchListener(null)
        effectiveDragListener = null
        ownedView = null
        backend = null
        currentState = OverlayState.DISPOSED
        PassThroughOpacityRegistry.unregister(this)
        val activityRelease = onActivityRelease
        onActivityRelease = null
        activityRelease?.invoke()
    }

    private fun accept(spec: OverlaySpec, dragListener: DraggableOnTouchListener<T>?) {
        effectiveSpec = spec
        effectiveDragListener = dragListener
        applyTouchListener(dragListener)
    }

    private fun nextDragListener(spec: OverlaySpec): DraggableOnTouchListener<T>? {
        if (spec.touchMode != OverlayTouchMode.DRAGGABLE) return null
        if (effectiveSpec.touchMode == OverlayTouchMode.DRAGGABLE && effectiveDragListener != null) {
            return effectiveDragListener
        }
        return DraggableOnTouchListener(this)
    }

    private fun applyTouchListener(listener: DraggableOnTouchListener<T>?) {
        ownedView?.setOnTouchListener(listener)
    }

    private fun layoutParams(value: OverlaySpec): WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
        width = value.width
        height = value.height
        gravity = Gravity.getAbsoluteGravity(value.gravity, view.layoutDirection)
        x = value.x
        y = value.y
        horizontalMargin = value.horizontalMargin
        verticalMargin = value.verticalMargin
        alpha = value.alpha
        format = android.graphics.PixelFormat.TRANSLUCENT
        flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        if (value.allowOutsideBounds) flags = flags or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
        if (value.touchMode == OverlayTouchMode.PASS_THROUGH) flags = flags or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
        type = if (scope == OverlayScope.ACTIVITY) WindowManager.LayoutParams.TYPE_APPLICATION else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY else WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        if (scope == OverlayScope.ACTIVITY && value.screenBrightness != null) screenBrightness = value.screenBrightness
    }

    private fun success(changed: Boolean, preserveDiagnostic: Boolean = false): OverlayResult {
        if (!preserveDiagnostic) currentFailure = null
        return OverlayResult(currentState, changed, null, null)
    }

    private fun successfulReconciliation(finalState: OverlayState = OverlayState.CONFIGURED): OverlayResult {
        currentState = finalState
        currentFailure = OverlayFailure.NOT_ATTACHED
        return OverlayResult(finalState, false, null, null)
    }

    private fun failure(kind: OverlayFailure, cause: Throwable?): OverlayResult {
        currentFailure = kind
        return OverlayResult(currentState, false, kind, cause)
    }

    private fun disposedFailure(): OverlayResult = OverlayResult(
        currentState,
        false,
        OverlayFailure.WINDOW_MANAGER_REJECTED,
        null,
    )

    private fun backendOrThrow(): OverlayWindowManager = checkNotNull(backend) { "OverlayView is disposed." }

    /**
     * The Android 12+ (API 31+) opacity-budget admission check. Applicable only to an
     * application-scope [OverlayTouchMode.PASS_THROUGH] candidate that opts into
     * [CrossUidPassThrough.WHEN_SYSTEM_ALLOWS]. Runs on every [show]/[update] that reaches this
     * point -- callers already skip a true no-op spec before calling here -- including an x/y,
     * size, gravity, or margin-only change: the platform maximum can change between operations, so
     * it is queried fresh every time rather than cached or skipped for a non-alpha change. Returns
     * the failure to report, or `null` to proceed with the platform call.
     */
    private fun admitPassThroughOpacity(candidate: OverlaySpec, managedView: T): OverlayFailure? {
        if (scope != OverlayScope.APPLICATION) return null
        if (candidate.touchMode != OverlayTouchMode.PASS_THROUGH) return null
        if (candidate.crossUidPassThrough != CrossUidPassThrough.WHEN_SYSTEM_ALLOWS) return null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return null
        val maximum = try {
            MaximumObscuringOpacity.get().maximumObscuringOpacityForTouch(managedView.context)
        } catch (exception: Exception) {
            null
        } ?: return OverlayFailure.PASS_THROUGH_UNSUPPORTED
        val combined = PassThroughOpacityRegistry.combinedOpacityExcluding(this, candidate.alpha)
        return if (combined > maximum) OverlayFailure.PASS_THROUGH_OPACITY_EXCEEDED else null
    }

    /** Registers or drops this handle's alpha in [PassThroughOpacityRegistry] after a successful backend call. */
    private fun updatePassThroughOpacityRegistration(spec: OverlaySpec) {
        if (scope == OverlayScope.APPLICATION && spec.touchMode == OverlayTouchMode.PASS_THROUGH) {
            PassThroughOpacityRegistry.register(this, spec.alpha)
        } else {
            PassThroughOpacityRegistry.unregister(this)
        }
    }

    private fun requireApplicationBrightnessIsAbsent(spec: OverlaySpec) {
        require(scope != OverlayScope.APPLICATION || spec.screenBrightness == null) {
            "screenBrightness is supported only for activity overlays."
        }
    }

    private fun classify(exception: Exception): OverlayFailure = when {
        isNotAttached(exception) -> OverlayFailure.NOT_ATTACHED
        exception is WindowManager.BadTokenException -> OverlayFailure.INVALID_WINDOW_TOKEN
        exception is SecurityException -> OverlayFailure.PERMISSION_DENIED
        else -> OverlayFailure.WINDOW_MANAGER_REJECTED
    }

    private fun isNotAttached(exception: Exception): Boolean = exception is IllegalArgumentException && exception.message?.contains("not attached", ignoreCase = true) == true

    private fun requireMainThread() {
        val main = Looper.getMainLooper()
        check(main != null && Looper.myLooper() === main) { "OverlayView must be used on the main thread." }
    }

}
