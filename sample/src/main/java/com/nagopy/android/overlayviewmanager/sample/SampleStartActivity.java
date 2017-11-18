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
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.nagopy.android.overlayviewmanager.OverlayViewManager;
import com.nagopy.android.overlayviewmanager.opt.timber.DebugOverlayTree;
import com.nagopy.android.overlayviewmanager.sample.databinding.ActivitySampleStartBinding;

import timber.log.Timber;

public class SampleStartActivity extends AppCompatActivity {

    ActivitySampleStartBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DebugOverlayTree.getInstance().register(this);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_sample_start);

        if (savedInstanceState == null) {
            if (!OverlayViewManager.canDrawOverlays()) {
                OverlayViewManager.showPermissionRequestDialog(getSupportFragmentManager(), R.string.app_name);
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Timber.d("canDrawOverlays:%s", OverlayViewManager.canDrawOverlays());
        binding.setCanDrawOverlays(OverlayViewManager.canDrawOverlays());
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_requestPermission:
                OverlayViewManager.requestOverlayPermission();
                break;
            case R.id.btn_sample1:
                startActivity(new Intent(this, Sample1Activity.class));
                break;
            case R.id.btn_sample2:
                startActivity(new Intent(this, Sample2Activity.class));
                break;
            case R.id.btn_sample_all_options:
                startActivity(new Intent(this, SampleAllOptionsActivity.class));
                break;
        }
    }

}
