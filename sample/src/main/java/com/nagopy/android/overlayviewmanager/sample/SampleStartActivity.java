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
import androidx.databinding.DataBindingUtil;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;

import com.nagopy.android.overlayviewmanager.OverlayViewManager;
import com.nagopy.android.overlayviewmanager.sample.databinding.ActivitySampleStartBinding;

import timber.log.Timber;

public class SampleStartActivity extends AppCompatActivity {

    ActivitySampleStartBinding binding;
    OverlayViewManager overlayViewManager;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_sample_start);
        overlayViewManager = OverlayViewManager.getInstance();

        if (savedInstanceState == null) {
            if (!overlayViewManager.canDrawOverlays()) {
                overlayViewManager.showPermissionRequestDialog(getSupportFragmentManager(), R.string.app_name);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Timber.d("canDrawOverlays:%s", overlayViewManager.canDrawOverlays());
        binding.setCanDrawOverlays(overlayViewManager.canDrawOverlays());
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_requestPermission:
                overlayViewManager.requestOverlayPermission();
                break;
            case R.id.btn_sample_all_options:
                startActivity(new Intent(this, SampleAllOptionsActivity.class));
                break;
            case R.id.btn_sample1:
                startActivity(new Intent(this, Sample1Activity.class));
                break;
            case R.id.btn_sample2:
                startActivity(new Intent(this, Sample2Activity.class));
                break;
            case R.id.btn_sample_timber:
                startActivity(new Intent(this, SampleTimberActivity.class));
                break;
            case R.id.btn_sample_override_brightness:
                startActivity(new Intent(this, SampleOverrideScreenBrightnessActivity.class));
                break;
        }
    }

}
