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
import android.widget.Button;

import androidx.annotation.Nullable;

import com.nagopy.android.overlayviewmanager.OverlayResult;
import com.nagopy.android.overlayviewmanager.OverlayViewManager;

import java.util.List;

import timber.log.Timber;

/**
 * Application-scoped overlay shown while a {@link Service} runs, on top of other apps, so it needs
 * the "display over other apps" permission for every show(). The handle itself is owned by the
 * application-level {@link Sample2OverlayController}, not by the Service, so a failed disposal
 * keeps a retry path after the Service is gone.
 */
public class Sample2Activity extends BaseSampleWithCodeActivity {

    private ActivityManager activityManager;
    private Button retryDisposeButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample2);
        activityManager = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        retryDisposeButton = findViewById(R.id.btn_retry_dispose);
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshRetryButton();
    }

    public void onClick(View view) {
        if (view.getId() == R.id.btn_retry_dispose) {
            // Explicit, user-driven retry of a disposal that failed when the Service stopped.
            OverlayResult disposed = Sample2OverlayController.get().dispose();
            if (!disposed.isSuccess()) {
                Timber.w(disposed.getCause(), "Retry of dispose() failed again: %s", disposed.getFailure());
            }
            refreshRetryButton();
        } else if (isServiceRunning()) {
            stopService(new Intent(this, Sample2Service.class));
            // The Service is destroyed asynchronously; look again shortly afterwards.
            view.postDelayed(this::refreshRetryButton, 500);
        } else {
            startService(new Intent(this, Sample2Service.class));
        }
    }

    private void refreshRetryButton() {
        boolean retained = Sample2OverlayController.get().hasHandle() && !isServiceRunning();
        retryDisposeButton.setVisibility(retained ? View.VISIBLE : View.GONE);
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

        @Override
        public void onCreate() {
            super.onCreate();
            // Application-scoped overlays always require the permission. The host decides what to
            // do when it is missing; this sample simply stops instead of asking the user again.
            if (!OverlayViewManager.getInstance().overlayPermission().isGranted(this)) {
                Timber.w("Overlay permission is not granted; Sample2Service stops without showing anything.");
                stopSelf();
                return;
            }
            OverlayResult shown = Sample2OverlayController.get().show(this);
            if (!shown.isSuccess()) {
                // e.g. PERMISSION_DENIED if the permission was revoked between the check and show().
                Timber.w(shown.getCause(), "Sample2Service show() failed: %s", shown.getFailure());
                stopSelf();
            }
        }

        @Override
        public void onDestroy() {
            OverlayResult disposed = Sample2OverlayController.get().dispose();
            if (!disposed.isSuccess()) {
                // The controller keeps the handle; Sample2Activity offers the explicit retry.
                Timber.w(disposed.getCause(), "Sample2Service dispose() failed: %s; handle retained for retry", disposed.getFailure());
            }
            super.onDestroy();
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }
    }
}
