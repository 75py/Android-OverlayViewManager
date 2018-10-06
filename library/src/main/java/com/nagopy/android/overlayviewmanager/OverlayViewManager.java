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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.fragment.app.FragmentManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.WindowManager;

import com.nagopy.android.overlayviewmanager.internal.Logger;
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowManager;
import com.nagopy.android.overlayviewmanager.internal.ScreenMonitor;

/**
 * Overlay your views!
 */
public final class OverlayViewManager {

    Context applicationContext;
    WindowManager windowManager;

    static OverlayViewManager INSTANCE = new OverlayViewManager();

    /**
     * Initialize.
     *
     * @param application Your {@link Application} instance
     */
    public static void init(Application application) {
        INSTANCE.initialize(application);
    }

    /**
     * Return the {@link OverlayViewManager} instance. The instance is singleton.
     *
     * @return instance
     */
    public static OverlayViewManager getInstance() {
        if (INSTANCE.applicationContext == null) {
            throw new IllegalStateException("OverlayViewManager is not initialized. Please call initApplicationInstance(Application).");
        }
        return INSTANCE;
    }

    OverlayViewManager() {
    }

    /**
     * Initialize. Please call this method before {@link OverlayView} use.
     *
     * @param context {@link Context}
     */
    void initialize(@NonNull Context context) {
        applicationContext = context.getApplicationContext();
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        OverlayWindowManager.initApplicationInstance(windowManager);
        ScreenMonitor.init(applicationContext);
    }

    /**
     * Wrapper of {@link Settings#canDrawOverlays(Context)}.
     *
     * @return true: This app context is allowed display over other apps
     */
    public boolean canDrawOverlays() {
        //noinspection SimplifiableIfStatement
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        } else {
            return Settings.canDrawOverlays(applicationContext);
        }
    }

    /**
     * Show permission request dialog.
     *
     * @param fragmentManager {@link FragmentManager}
     * @param appNameId       String resource id of application name
     */
    @TargetApi(Build.VERSION_CODES.M)
    public void showPermissionRequestDialog(@NonNull FragmentManager fragmentManager, @StringRes int appNameId) {
        PermissionRequestDialogFragment dialog = PermissionRequestDialogFragment.newInstance(appNameId);
        dialog.show(fragmentManager, "PermissionRequestDialogFragment");
    }

    /**
     * Request overlay permission. Open the setting app.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public void requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!canDrawOverlays()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                        , Uri.parse("package:" + applicationContext.getPackageName()));
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                applicationContext.startActivity(intent);
            }
        } else {
            Logger.d("API level %d is not required any user action.", Build.VERSION.SDK_INT);
        }
    }

    /**
     * Get display width.
     *
     * @return Display width(px)
     */
    public int getDisplayWidth() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    /**
     * Get display height. It includes the status bar and the navigation bar.
     *
     * @return Display height(px)
     */
    public int getDisplayHeight() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels;
    }

    /**
     * Create a new OverlayView instance. ViewScope = ACTIVITY
     * If you want to show your view in Activity's lifecycle, use #newOverlayView(View, Activity).
     * Otherwise(If you want to show your view regardless of whether Activity is alive), use #newOverlayView(View).
     * <p>
     * If you use OverlayView from Activity only, you don't have to get "Draw over other apps" permission.
     *
     * @param view     The view that you want to overlay
     * @param <T>      View
     * @param activity Parent activity
     * @return A new instance of OverlayView
     */
    @NonNull
    public <T extends View> OverlayView<T> newOverlayView(T view, Activity activity) {
        return OverlayView.create(view, activity);
    }

    /**
     * Create a new OverlayView instance. ViewScope = APPLICATION
     * If you want to show your view regardless of whether Activity is alive, use #newOverlayView(View).
     * Otherwise(if you want to show your view in Activity's lifecycle), use #newOverlayView(View, Activity).
     *
     * @param view The view that you want to overlay
     * @param <T>  View
     * @return A new instance of OverlayView
     */
    @NonNull
    public <T extends View> OverlayView<T> newOverlayView(T view) {
        return OverlayView.create(view);
    }
}
