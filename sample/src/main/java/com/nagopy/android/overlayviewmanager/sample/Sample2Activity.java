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
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.ImageView;

import com.nagopy.android.overlayviewmanager.OverlayView;

import java.util.List;

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
            overlayView = OverlayView.create(createImageView())
                    .setTouchable(true)
                    .setDraggable(true)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            startActivity(new Intent(getApplicationContext(), Sample2Activity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        }
                    }).show();
        }

        @Override
        public void onDestroy() {
            overlayView.hide();
            super.onDestroy();
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        ImageView createImageView() {
            ImageView imageView = new ImageView(this);
            imageView.setId(R.id.sample_text_view);
            imageView.setImageResource(R.mipmap.ic_launcher);
            return imageView;
        }
    }
}
