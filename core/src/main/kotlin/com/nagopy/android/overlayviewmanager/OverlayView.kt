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

import android.app.Activity
import android.os.Build
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.annotation.MainThread
import androidx.annotation.VisibleForTesting
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowManager

/** A synchronous, main-thread-owned overlay handle. */
public class OverlayView<T : View> internal constructor(
    view: T,
    private val scope: OverlayScope,
    private val backend: OverlayWindowManager,
    initialSpec: OverlaySpec,
) {
    private var ownedView: T? = view
    private var detachListener: View.OnAttachStateChangeListener? = null
    private var pendingSpec: OverlaySpec = initialSpec
    private var effectiveSpec: OverlaySpec = initialSpec
    @Volatile private var currentState: OverlayState = OverlayState.CONFIGURED
    @Volatile private var currentFailure: OverlayFailure? = null
    private var libraryRemovalInProgress: Boolean = false

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
        val candidate = pendingSpec
        val managedView = ownedView ?: return failure(OverlayFailure.WINDOW_MANAGER_REJECTED, null)
        if (managedView.parent != null) return failure(OverlayFailure.ALREADY_HAS_PARENT, null)
        if (scope == OverlayScope.APPLICATION && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(managedView.context)) {
            return failure(OverlayFailure.PERMISSION_DENIED, null)
        }
        try {
            backend.show(managedView, layoutParams(candidate))
        } catch (throwable: Throwable) {
            return failure(classify(throwable), throwable)
        }
        effectiveSpec = candidate
        currentState = OverlayState.ATTACHED
        installDetachListener(managedView)
        return success(changed = true)
    }

    /** Applies [spec] synchronously or replaces pending configuration before first show. */
    @MainThread
    public fun update(spec: OverlaySpec): OverlayResult {
        requireMainThread()
        if (currentState == OverlayState.DISPOSED) return disposedFailure()
        if (currentState == OverlayState.CONFIGURED) {
            if (spec == effectiveSpec) return success(changed = false)
            pendingSpec = spec
            effectiveSpec = spec
            return success(changed = true)
        }
        if (spec == effectiveSpec) return success(changed = false)
        val managedView = ownedView ?: return failure(OverlayFailure.WINDOW_MANAGER_REJECTED, null)
        try {
            backend.update(managedView, layoutParams(spec))
        } catch (throwable: Throwable) {
            if (isNotAttached(throwable)) {
                reconcileExternalDetach()
                return OverlayResult(currentState, false, OverlayFailure.NOT_ATTACHED, throwable)
            }
            return failure(classify(throwable), throwable)
        }
        pendingSpec = spec
        effectiveSpec = spec
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
            backend.hide(managedView)
        } catch (throwable: Throwable) {
            libraryRemovalInProgress = false
            if (isNotAttached(throwable)) {
                reconcileExternalDetach()
                return successfulReconciliation()
            }
            return failure(classify(throwable), throwable)
        }
        libraryRemovalInProgress = false
        currentState = OverlayState.CONFIGURED
        removeDetachListener(managedView)
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
                backend.hide(managedView)
            } catch (throwable: Throwable) {
                libraryRemovalInProgress = false
                if (isNotAttached(throwable)) {
                    reconcileExternalDetach()
                    release(managedView)
                    return successfulReconciliation(OverlayState.DISPOSED)
                }
                return failure(classify(throwable), throwable)
            }
            libraryRemovalInProgress = false
            release(managedView)
            return success(changed = true)
        }
        ownedView?.let(::release)
        return success(changed = true, preserveDiagnostic = true)
    }

    /**
     * Temporary 2.x migration bridge. It changes pending configuration only; callers must invoke
     * [update] to apply it, so [spec] never reports a rejected layout as applied.
     */
    @Deprecated("Use update(OverlaySpec) instead.")
    @MainThread
    public fun update(): OverlayResult = update(pendingSpec)

    /** Temporary 2.x migration bridge. */
    @Deprecated("Use OverlaySpec.touchMode instead.")
    @MainThread
    public fun setTouchable(touchable: Boolean): OverlayView<T> = setPending {
        it.copy(touchMode = if (touchable) OverlayTouchMode.INTERACTIVE else OverlayTouchMode.PASS_THROUGH)
    }

    /** Temporary 2.x migration bridge. */
    @Deprecated("Use OverlaySpec.touchMode instead.")
    @MainThread
    public fun setDraggable(draggable: Boolean): OverlayView<T> {
        requireMainThread()
        view.setOnTouchListener(if (draggable) DraggableOnTouchListener(this) else null)
        return setPending { it.copy(touchMode = if (draggable) OverlayTouchMode.DRAGGABLE else OverlayTouchMode.PASS_THROUGH) }
    }

    /** Temporary 2.x migration bridge; custom drag listeners are not a 3.0 API. */
    @Deprecated("Use OverlaySpec.touchMode instead.")
    @MainThread
    public fun setDraggable(draggable: Boolean, listener: DraggableOnTouchListener<T>): OverlayView<T> {
        requireMainThread()
        view.setOnTouchListener(if (draggable) listener else null)
        return setPending { it.copy(touchMode = if (draggable) OverlayTouchMode.DRAGGABLE else OverlayTouchMode.PASS_THROUGH) }
    }

    @Deprecated("Use OverlaySpec.width instead.") @MainThread public fun setWidth(value: Int): OverlayView<T> = setPending { it.copy(width = value) }
    @Deprecated("Use OverlaySpec.height instead.") @MainThread public fun setHeight(value: Int): OverlayView<T> = setPending { it.copy(height = value) }
    @Deprecated("Use OverlaySpec.gravity instead.") @MainThread public fun setGravity(value: Int): OverlayView<T> = setPending { it.copy(gravity = value) }
    @Deprecated("Use OverlaySpec.x instead.") @MainThread public fun setX(value: Int): OverlayView<T> = setPending { it.copy(x = value) }
    @Deprecated("Use OverlaySpec.y instead.") @MainThread public fun setY(value: Int): OverlayView<T> = setPending { it.copy(y = value) }
    @Deprecated("Use OverlaySpec.alpha instead.") @MainThread public fun setAlpha(value: Float): OverlayView<T> = setPending { it.copy(alpha = value) }
    @Deprecated("Use OverlaySpec.horizontalMargin instead.") @MainThread public fun setHorizontalMargin(value: Float): OverlayView<T> = setPending { it.copy(horizontalMargin = value) }
    @Deprecated("Use OverlaySpec.verticalMargin instead.") @MainThread public fun setVerticalMargin(value: Float): OverlayView<T> = setPending { it.copy(verticalMargin = value) }
    @Deprecated("Use OverlaySpec.screenBrightness instead.") @MainThread public fun setScreenBrightness(value: Float): OverlayView<T> = setPending { it.copy(screenBrightness = value) }
    @Deprecated("No 3.0 replacement is available.") @MainThread public fun allowViewToExtendOutsideScreen(value: Boolean): OverlayView<T> = setPending { it.copy(allowOutsideBounds = value) }
    @Deprecated("Use state == OverlayState.ATTACHED instead.") public fun isVisible(): Boolean = state == OverlayState.ATTACHED
    @Deprecated("Use spec.x instead.") @MainThread public fun getX(): Int = spec.x
    @Deprecated("Use spec.y instead.") @MainThread public fun getY(): Int = spec.y
    @Deprecated("Set the listener on view instead.") @MainThread public fun setOnClickListener(listener: View.OnClickListener?): OverlayView<T> {
        requireMainThread(); view.setOnClickListener(listener); return this
    }

    @VisibleForTesting
    internal fun pendingSpecForTesting(): OverlaySpec = pendingSpec

    private fun setPending(transform: (OverlaySpec) -> OverlaySpec): OverlayView<T> {
        requireMainThread()
        check(currentState != OverlayState.DISPOSED) { "OverlayView is disposed." }
        pendingSpec = transform(pendingSpec)
        return this
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
    }

    private fun release(managedView: T) {
        removeDetachListener(managedView)
        managedView.setOnTouchListener(null)
        ownedView = null
        currentState = OverlayState.DISPOSED
    }

    private fun layoutParams(value: OverlaySpec): WindowManager.LayoutParams = WindowManager.LayoutParams().apply {
        width = value.width
        height = value.height
        gravity = GravityCompat.getAbsoluteGravity(value.gravity, ViewCompat.getLayoutDirection(view))
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

    private fun classify(throwable: Throwable): OverlayFailure = when {
        isNotAttached(throwable) -> OverlayFailure.NOT_ATTACHED
        throwable is WindowManager.BadTokenException -> OverlayFailure.INVALID_WINDOW_TOKEN
        throwable is SecurityException -> OverlayFailure.PERMISSION_DENIED
        else -> OverlayFailure.WINDOW_MANAGER_REJECTED
    }

    private fun isNotAttached(throwable: Throwable): Boolean = throwable is IllegalArgumentException && throwable.message?.contains("not attached", ignoreCase = true) == true

    private fun requireMainThread() {
        val main = Looper.getMainLooper()
        check(main == null || Looper.myLooper() === main) { "OverlayView must be used on the main thread." }
    }
}
