/*
 * Copyright 2017 75py
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

import android.app.Activity;
import android.graphics.PixelFormat;
import android.os.Build;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import com.nagopy.android.overlayviewmanager.internal.Logger;
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowManager;
import com.nagopy.android.overlayviewmanager.internal.ScreenMonitor;

import androidx.annotation.FloatRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;

/**
 * The wrapper class of the view to overlay.
 * This class allows a view to be displayed as an overlay on top of other content.
 *
 * @param <T> The type of the view to be overlaid.
 */
public class OverlayView<T extends View> {

    @VisibleForTesting
    T view;
    @VisibleForTesting
    WindowManager.LayoutParams params;
    @VisibleForTesting
    boolean isVisible;

    @VisibleForTesting
    boolean isDraggable;
    @VisibleForTesting
    boolean isTouchable;
    @VisibleForTesting
    boolean allowViewToExtendOutsideScreen;

    @VisibleForTesting
    int lastWindowType;

    @VisibleForTesting
    OverlayWindowManager overlayWindowManager;

    @VisibleForTesting
    ScreenMonitor screenMonitor;

    enum ViewScope {APPLICATION, ACTIVITY}

    @VisibleForTesting
    ViewScope viewScope;

    /**
     * Create a new instance with ViewScope set to APPLICATION.
     * This is used when the overlay is to be displayed regardless of the activity lifecycle.
     *
     * @param view The view to overlay.
     * @param <T>  The type of the view.
     * @return A new instance of OverlayView.
     */
    @NonNull
    static <T extends View> OverlayView<T> create(@NonNull T view) {
        OverlayView<T> overlayView = new OverlayView<>(view);
        overlayView.viewScope = ViewScope.APPLICATION;
        overlayView.overlayWindowManager = OverlayWindowManager.getApplicationInstance();
        return overlayView;
    }

    /**
     * Create a new instance with ViewScope set to ACTIVITY.
     * This is used when the overlay should be managed within an activity's lifecycle.
     *
     * @param view     The view to overlay.
     * @param <T>      The type of the view.
     * @param activity The parent activity.
     * @return A new instance of OverlayView.
     */
    @NonNull
    static <T extends View> OverlayView<T> create(@NonNull T view, Activity activity) {
        OverlayView<T> overlayView = new OverlayView<>(view);
        overlayView.viewScope = ViewScope.ACTIVITY;
        overlayView.overlayWindowManager = OverlayWindowManager.getActivityInstance(activity);
        return overlayView;
    }

    /**
     * Private constructor.
     * Use {@link OverlayView#create(View)} or {@link OverlayView#create(View, Activity)} to get an instance.
     *
     * @param view The view to overlay.
     */
    private OverlayView(@NonNull T view) {
        this.view = view;
        params = new WindowManager.LayoutParams();
        params.format = PixelFormat.TRANSLUCENT;
        setWidth(WRAP_CONTENT)
                .setHeight(WRAP_CONTENT)
                .setTouchable(false)
                .setDraggable(false)
                .allowViewToExtendOutsideScreen(false)
                .setGravity(Gravity.TOP | Gravity.START);
        isVisible = false;
        screenMonitor = ScreenMonitor.getInstance();
    }

    /**
     * Starts overlaying the view.
     * If the view is already shown, this method does nothing.
     *
     * @return This object for method chaining.
     */
    @NonNull
    public synchronized OverlayView<T> show() {
        if (!isVisible()) {
            updateParams();

            overlayWindowManager.show(view, params);
            isVisible = true;
            lastWindowType = params.type;

            if (isDraggable) {
                screenMonitor.request(this);
            }
        }
        return this;
    }

    /**
     * Requests updating the window parameters.
     * If the view is shown, it will call {@link WindowManager#updateViewLayout(View, ViewGroup.LayoutParams)}.
     */
    public synchronized void update() {
        if (isVisible()) {
            updateParams();

            if (lastWindowType == params.type) {
                overlayWindowManager.update(view, params);
            } else {
                Logger.d("Window type is changed. %d => %d", lastWindowType, params.type);
                overlayWindowManager.hide(view);
                overlayWindowManager.show(view, params);
                lastWindowType = params.type;
            }

            if (isDraggable) {
                screenMonitor.request(this);
            } else {
                screenMonitor.cancel(this);
            }
        }
    }

    /**
     * Stops overlaying the view.
     * If the view is already hidden, this method does nothing.
     */
    public synchronized void hide() {
        if (isVisible()) {
            overlayWindowManager.hide(view);
            isVisible = false;

            if (isDraggable) {
                screenMonitor.cancel(this);
            }
        }
    }

    /**
     * Returns the managed view.
     *
     * @return The overlaid view.
     */
    @NonNull
    public T getView() {
        return view;
    }

    /**
     * Returns the visibility status of the view.
     *
     * @return true if the view is visible, false otherwise.
     */
    public boolean isVisible() {
        return isVisible;
    }

    /**
     * Registers a callback to click events.
     *
     * @param onClickListener The callback to be invoked on click events.
     * @return This object for method chaining.
     */
    @NonNull
    public OverlayView<T> setOnClickListener(@Nullable View.OnClickListener onClickListener) {
        view.setOnClickListener(onClickListener);
        return this;
    }

    /**
     * Enables or disables touch events for this view.
     *
     * @param touchable true to enable touch events, false to disable.
     * @return This object for method chaining.
     */
    @NonNull
    public OverlayView<T> setTouchable(boolean touchable) {
        this.isTouchable = touchable;
        return this;
    }

    /**
     * Allows or disallows the view to extend outside the screen.
     * If true, the view can overlay on a navigation bar or a status bar.
     * Note: Using this option may cause unexpected behavior.
     *
     * @param allowViewToExtendOutsideScreen true to allow extending, false otherwise.
     * @return This object for method chaining.
     */
    @NonNull
    public OverlayView<T> allowViewToExtendOutsideScreen(boolean allowViewToExtendOutsideScreen) {
        this.allowViewToExtendOutsideScreen = allowViewToExtendOutsideScreen;
        return this;
    }

    /**
     * Enables or disables moving the view by dragging.
     *
     * @param draggable true to enable dragging, false to disable.
     * @return This object for method chaining.
     */
    @NonNull
    public OverlayView<T> setDraggable(boolean draggable) {
        return setDraggable(draggable, new DraggableOnTouchListener<>(this));
    }

    /**
     * Enables or disables moving the view by dragging with a custom listener.
     *
     * @param draggable true to enable dragging, false to disable.
     * @param listener  The custom touch listener for handling drag events.
     * @return This object for method chaining.
     */
    @NonNull
    public OverlayView<T> setDraggable(boolean draggable, DraggableOnTouchListener<T> listener) {
        this.isDraggable = draggable;
        if (draggable) {
            view.setOnTouchListener(listener);
        } else {
            view.setOnTouchListener(null);
        }
        return this;
    }

    /**
     * Sets the width parameter of the view.
     *
     * @param width The width in pixels, or use {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT}
     *              or {@link android.view.ViewGroup.LayoutParams#MATCH_PARENT}.
     * @return This object for method chaining.
     */
    @NonNull
    public OverlayView<T> setWidth(int width) {
        params.width = width;
        return this;
    }

    /**
     * Sets the height parameter of the view.
     *
     * @param height The height in pixels, or use {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT}
     *               or {@link android.view.ViewGroup.LayoutParams#MATCH_PARENT}.
     * @return This object for method chaining.
     */
    @NonNull
    public OverlayView<T> setHeight(int height) {
        params.height = height;
        return this;
    }

    /**
     * Sets the placement of the window within the screen as per Gravity.
     *
     * @param gravity The gravity constants from {@link Gravity}.
     * @return This object for method chaining.
     * @see Gravity
     * @see WindowManager.LayoutParams#gravity
     */
    @NonNull
    public OverlayView<T> setGravity(int gravity) {
        params.gravity = GravityCompat.getAbsoluteGravity(gravity,
                ViewCompat.getLayoutDirection(view));
        return this;
    }

    /**
     * Sets the horizontal margin.
     *
     * @param margin The horizontal margin (0 to 1).
     * @return This object for method chaining.
     * @see WindowManager.LayoutParams#horizontalMargin
     */
    @NonNull
    public OverlayView<T> setHorizontalMargin(@FloatRange(from = 0.0, to = 1.0) float margin) {
        params.horizontalMargin = margin;
        return this;
    }

    /**
     * Sets the vertical margin.
     *
     * @param margin The vertical margin (0 to 1).
     * @return This object for method chaining.
     * @see WindowManager.LayoutParams#verticalMargin
     */
    @NonNull
    public OverlayView<T> setVerticalMargin(@FloatRange(from = 0.0, to = 1.0) float margin) {
        params.verticalMargin = margin;
        return this;
    }

    /**
     * Sets the opacity of the view. 0 means completely translucent.
     *
     * @param alpha The opacity value (0 to 1).
     * @return This object for method chaining.
     * @see WindowManager.LayoutParams#alpha
     */
    @NonNull
    public OverlayView<T> setAlpha(@FloatRange(from = 0.0, to = 1.0) float alpha) {
        params.alpha = alpha;
        return this;
    }

    /**
     * Sets the X position of the view.
     *
     * @param x The X position in pixels.
     * @return This object for method chaining.
     * @see WindowManager.LayoutParams#x
     */
    @NonNull
    public OverlayView<T> setX(int x) {
        params.x = x;
        return this;
    }

    /**
     * Gets the X position of the view.
     *
     * @return The X position in pixels.
     * @see WindowManager.LayoutParams#x
     */
    public int getX() {
        return params.x;
    }

    /**
     * Sets the Y position of the view.
     *
     * @param y The Y position in pixels.
     * @return This object for method chaining.
     * @see WindowManager.LayoutParams#y
     */
    @NonNull
    public OverlayView<T> setY(int y) {
        params.y = y;
        return this;
    }

    /**
     * Gets the Y position of the view.
     *
     * @return The Y position in pixels.
     * @see WindowManager.LayoutParams#y
     */
    public int getY() {
        return params.y;
    }

    /**
     * Sets the brightness of the screen.
     *
     * @param screenBrightness The brightness value (0 to 1).
     * @return This object for method chaining.
     * @see WindowManager.LayoutParams#screenBrightness
     */
    @NonNull
    public OverlayView<T> setScreenBrightness(float screenBrightness) {
        params.screenBrightness = screenBrightness;
        return this;
    }

    @VisibleForTesting
    void updateParams() {
        params.type = TYPE_APPLICATION;
        params.flags = FLAG_NOT_FOCUSABLE;

        if (allowViewToExtendOutsideScreen) {
            params.flags |= FLAG_LAYOUT_NO_LIMITS;
            params.flags |= FLAG_LAYOUT_INSET_DECOR;
            params.flags |= FLAG_LAYOUT_IN_SCREEN;
        }

        if (isTouchable || isDraggable) {
            if (allowViewToExtendOutsideScreen && Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
                // On the navigation
                params.type = TYPE_SYSTEM_ERROR;
            } else {
                // Touchable, cannot overlay on the navigation
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    params.type = TYPE_APPLICATION_OVERLAY;
                } else {
                    params.type = TYPE_SYSTEM_ALERT;
                }
            }
        } else {
            params.flags |= FLAG_NOT_TOUCHABLE;
            params.flags |= FLAG_NOT_TOUCH_MODAL;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                params.type = TYPE_APPLICATION_OVERLAY;
            } else {
                params.type = TYPE_SYSTEM_OVERLAY;
            }
        }

        if (viewScope == ViewScope.ACTIVITY) {
            params.type = TYPE_APPLICATION;
        }
    }

}
