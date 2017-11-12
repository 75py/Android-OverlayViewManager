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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.FragmentManager;
import android.util.DisplayMetrics;
import android.view.WindowManager;

import com.nagopy.android.overlayviewmanager.internal.Logger;
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowManager;
import com.nagopy.android.overlayviewmanager.internal.ScreenMonitor;

/**
 * Overlay your views!
 */
public final class OverlayViewManager {

    static Context applicationContext;
    private static WindowManager windowManager;

    private OverlayViewManager() {
    }

    /**
     * Initialize. Please call this method before {@link OverlayView} use.
     *
     * @param context {@link Context}
     */
    public static void init(@NonNull Context context) {
        applicationContext = context.getApplicationContext();
        windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        OverlayWindowManager.init(windowManager);
        ScreenMonitor.init(applicationContext);
    }

    /**
     * Wrapper of {@link Settings#canDrawOverlays(Context)}.
     *
     * @return true: This app context is allowed display over other apps
     */
    public static boolean canDrawOverlays() {
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
     * @param fragmentManager {@link android.support.v4.app.FragmentManager}
     * @param appNameId       String resource id of application name
     */
    @TargetApi(Build.VERSION_CODES.M)
    public static void showPermissionRequestDialog(@NonNull FragmentManager fragmentManager, @StringRes int appNameId) {
        PermissionRequestDialogFragment dialog = PermissionRequestDialogFragment.newInstance(appNameId);
        dialog.show(fragmentManager, "PermissionRequestDialogFragment");
    }

    /**
     * Request overlay permission. Open the setting app.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public static void requestOverlayPermission() {
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
    public static int getDisplayWidth() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics.widthPixels;
    }

    /**
     * Get display height. It includes the status bar and the navigation bar.
     *
     * @return Display height(px)
     */
    public static int getDisplayHeight() {
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        return metrics.heightPixels;
    }

}
