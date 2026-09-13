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
package com.nagopy.android.overlayviewmanager.sample;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.nagopy.android.overlayviewmanager.OverlayResult;
import com.nagopy.android.overlayviewmanager.OverlaySpec;
import com.nagopy.android.overlayviewmanager.OverlayTouchMode;
import com.nagopy.android.overlayviewmanager.OverlayView;
import com.nagopy.android.overlayviewmanager.OverlayViewManager;

import java.util.List;

import timber.log.Timber;

/**
 * Application-scoped overlay owned by a {@link Service}: it stays on screen while the Service runs,
 * on top of other apps, so it needs the "display over other apps" permission for every show().
 */
public class Sample2Activity extends BaseSampleWithCodeActivity {

    private ActivityManager activityManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample2);
        activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
    }

    public void onClick(View view) {
        if (isServiceRunning()) {
            stopService(new Intent(this, Sample2Service.class));
        } else {
            startService(new Intent(this, Sample2Service.class));
        }
    }

    private boolean isServiceRunning() {
        List<ActivityManager.RunningServiceInfo> runningServices = activityManager.getRunningServices(Integer.MAX_VALUE);
        String className = Sample2Service.class.getName();
        for (ActivityManager.RunningServiceInfo service : runningServices) {
            if (service.service.getClassName().equals(className)) {
                return true;
            }
        }
        return false;
    }

    public static class Sample2Service extends Service {

        OverlayView<ImageView> overlayView;

        @Override
        public void onCreate() {
            super.onCreate();
            OverlayViewManager manager = OverlayViewManager.getInstance();
            // Application-scoped overlays always require the permission. The host decides what to
            // do when it is missing; this sample simply stops instead of asking the user again.
            if (!manager.overlayPermission().isGranted(this)) {
                Timber.w("Overlay permission is not granted; Sample2Service stops without showing anything.");
                stopSelf();
                return;
            }

            ImageView imageView = createImageView();
            // Ordinary click delivery stays on the View itself, even while DRAGGABLE.
            imageView.setOnClickListener(v -> startActivity(
                    new Intent(getApplicationContext(), Sample2Activity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));

            overlayView = manager.newOverlayView(
                    imageView,
                    new OverlaySpec.Builder().setTouchMode(OverlayTouchMode.DRAGGABLE).build());
            OverlayResult shown = overlayView.show();
            if (!shown.isSuccess()) {
                // e.g. PERMISSION_DENIED if the permission was revoked between the check and show().
                Timber.w(shown.getCause(), "Sample2Service show() failed: %s", shown.getFailure());
                stopSelf();
            }
        }

        @Override
        public void onDestroy() {
            if (overlayView != null) {
                OverlayResult disposed = overlayView.dispose();
                if (!disposed.isSuccess()) {
                    // A failed disposal keeps the handle retryable; a Service that is being destroyed
                    // can only report it.
                    Timber.w(disposed.getCause(), "Sample2Service dispose() failed: %s", disposed.getFailure());
                }
            }
            super.onDestroy();
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        ImageView createImageView() {
            ImageView imageView = new ImageView(this);
            imageView.setId(R.id.sample_image_view);
            imageView.setImageResource(R.mipmap.ic_launcher);
            return imageView;
        }
    }
}
