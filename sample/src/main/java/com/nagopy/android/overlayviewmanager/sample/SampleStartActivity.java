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

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.databinding.DataBindingUtil;

import com.nagopy.android.overlayviewmanager.OverlayPermission;
import com.nagopy.android.overlayviewmanager.OverlayViewManager;
import com.nagopy.android.overlayviewmanager.sample.databinding.ActivitySampleStartBinding;

import timber.log.Timber;

/**
 * Entry screen. Shows how the host application owns the "display over other apps" permission
 * flow: check with {@link OverlayPermission#isGranted}, open the system settings screen through an
 * Activity Result launcher, and re-check when the user comes back. The library never opens that
 * screen on its own and never retries a show() for you.
 */
public class SampleStartActivity extends AppCompatActivity {

    ActivitySampleStartBinding binding;
    OverlayPermission overlayPermission;

    // The system settings screen returns no payload; the only reliable signal is to re-check the
    // permission once the user is back.
    private final ActivityResultLauncher<Intent> permissionSettingsLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> refreshPermissionState());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sample_start);
        overlayPermission = OverlayViewManager.getInstance().overlayPermission();
    }

    @Override
    protected void onStart() {
        super.onStart();
        refreshPermissionState();
    }

    public void onClick(View view) {
        if (view.getId() == R.id.btn_requestPermission) {
            permissionSettingsLauncher.launch(overlayPermission.settingsIntent(this));
        } else if (view.getId() == R.id.btn_sample_all_options) {
            startActivity(new Intent(this, SampleAllOptionsActivity.class));
        } else if (view.getId() == R.id.btn_sample1) {
            startActivity(new Intent(this, Sample1Activity.class));
        } else if (view.getId() == R.id.btn_sample2) {
            startActivity(new Intent(this, Sample2Activity.class));
        } else if (view.getId() == R.id.btn_sample_timber) {
            startActivity(new Intent(this, SampleTimberActivity.class));
        } else if (view.getId() == R.id.btn_sample_override_brightness) {
            startActivity(new Intent(this, SampleOverrideScreenBrightnessActivity.class));
        }
    }

    private void refreshPermissionState() {
        boolean granted = overlayPermission.isGranted(this);
        Timber.d("overlay permission granted: %s", granted);
        binding.setCanDrawOverlays(granted);
    }
}
