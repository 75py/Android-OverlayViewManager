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
 *
 * @param <T> View
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
     * Create a new instance. ViewScope = APPLICATION
     *
     * @param view The view that you want to overlay
     * @param <T>  View
     * @return A new instance
     */
    @NonNull
    static <T extends View> OverlayView<T> create(@NonNull T view) {
        OverlayView<T> overlayView = new OverlayView<>(view);
        overlayView.viewScope = ViewScope.APPLICATION;
        overlayView.overlayWindowManager = OverlayWindowManager.getApplicationInstance();
        return overlayView;
    }

    /**
     * Create a new instance. ViewScope = ACTIVITY
     *
     * @param view     The view that you want to overlay
     * @param <T>      View
     * @param activity Parent activity
     * @return A new instance
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
     * You can get the instance by {@link OverlayView#create(View)} method.
     *
     * @param view The view that you want to overlay
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
     * Start overlay.
     * If the view is already shown, it will do anything.
     *
     * @return This object
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
     * Request updating window parameters.
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
     * Stop overlay.
     * If the view is already hidden, it will do anything.
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
     * Return the managed view.
     *
     * @return view
     */
    @NonNull
    public T getView() {
        return view;
    }

    /**
     * Return view's visibility.
     *
     * @return true: The view is visible.
     */
    public boolean isVisible() {
        return isVisible;
    }

    /**
     * Register a callback to click events.
     *
     * @param onClickListener the callback
     * @return This object
     */
    @NonNull
    public OverlayView<T> setOnClickListener(@Nullable View.OnClickListener onClickListener) {
        view.setOnClickListener(onClickListener);
        return this;
    }

    /**
     * Enables or disables touch events for this view.
     *
     * @param touchable true:touchable
     * @return This object
     */
    @NonNull
    public OverlayView<T> setTouchable(boolean touchable) {
        this.isTouchable = touchable;
        return this;
    }

    /**
     * Allow the view to extend outside the screen or not.
     * If true, the view can overlay on a navigation bar or a status bar.
     * If you use this method and show a wrong size view, you cannot do anything. Please be careful.
     *
     * @param allowViewToExtendOutsideScreen true: allow the view to extend outside the screen
     * @return This object
     */
    @NonNull
    public OverlayView<T> allowViewToExtendOutsideScreen(boolean allowViewToExtendOutsideScreen) {
        this.allowViewToExtendOutsideScreen = allowViewToExtendOutsideScreen;
        return this;
    }

    /**
     * Enables or disables moving by drag.
     *
     * @param draggable true:draggable
     * @return This object
     */
    @NonNull
    public OverlayView<T> setDraggable(boolean draggable) {
        return setDraggable(draggable, new DraggableOnTouchListener<>(this));
    }

    /**
     * Enables or disables moving by drag.
     *
     * @param draggable true:draggable
     * @param listener  DraggableOnTouchListener
     * @return This object
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
     * Set the width parameter of the view.
     *
     * @param width Specify width in pixels,
     *              otherwise use {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT}
     *              or {@link android.view.ViewGroup.LayoutParams#MATCH_PARENT}.
     * @return This object
     * @see android.view.ViewGroup.LayoutParams#width
     */
    @NonNull
    public OverlayView<T> setWidth(int width) {
        params.width = width;
        return this;
    }

    /**
     * Set the height parameter of the view.
     *
     * @param height Specify height in pixels,
     *               otherwise use {@link android.view.ViewGroup.LayoutParams#WRAP_CONTENT}
     *               or {@link android.view.ViewGroup.LayoutParams#MATCH_PARENT}.
     * @return This object
     * @see android.view.ViewGroup.LayoutParams#height
     */
    @NonNull
    public OverlayView<T> setHeight(int height) {
        params.height = height;
        return this;
    }


    /**
     * Placement of window within the screen as per Gravity.
     *
     * @param gravity The constants in {@link Gravity} class
     * @return This object
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
     * Set the horizontal margin.
     *
     * @param margin The horizontal margin(0 to 1)
     * @return This object
     * @see WindowManager.LayoutParams#horizontalMargin
     */
    @NonNull
    public OverlayView<T> setHorizontalMargin(@FloatRange(from = 0.0, to = 1.0) float margin) {
        params.horizontalMargin = margin;
        return this;
    }

    /**
     * Set the vertical margin.
     *
     * @param margin The vertical margin(0 to 1)
     * @return This object
     * @see WindowManager.LayoutParams#verticalMargin
     */
    @NonNull
    public OverlayView<T> setVerticalMargin(@FloatRange(from = 0.0, to = 1.0) float margin) {
        params.verticalMargin = margin;
        return this;
    }

    /**
     * Set the opacity of the view. 0 means completely translucent.
     *
     * @param alpha opacity(0 to 1)
     * @return This object
     * @see WindowManager.LayoutParams#alpha
     */
    @NonNull
    public OverlayView<T> setAlpha(@FloatRange(from = 0.0, to = 1.0) float alpha) {
        params.alpha = alpha;
        return this;
    }

    /**
     * Set the X position.
     *
     * @param x X position. The unit is pixels.
     * @return This object
     * @see WindowManager.LayoutParams#x
     */
    @NonNull
    public OverlayView<T> setX(int x) {
        params.x = x;
        return this;
    }

    /**
     * Get the X position.
     *
     * @return X position. The unit is pixels.
     * @see WindowManager.LayoutParams#x
     */
    public int getX() {
        return params.x;
    }

    /**
     * Set the Y position.
     *
     * @param y Y position. The unit is pixels.
     * @return This object
     * @see WindowManager.LayoutParams#y
     */
    @NonNull
    public OverlayView<T> setY(int y) {
        params.y = y;
        return this;
    }

    /**
     * Get the Y position.
     *
     * @return Y position. The unit is pixels.
     * @see WindowManager.LayoutParams#y
     */
    public int getY() {
        return params.y;
    }

    /**
     * Set the brightness of the screen.
     *
     * @param screenBrightness 0 to 1 adjusts the brightness from dark to full bright
     * @return This object
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
