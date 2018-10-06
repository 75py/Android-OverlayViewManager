/*
 * Copyright 2018 75py
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

import androidx.databinding.DataBindingUtil;
import androidx.databinding.adapters.SeekBarBindingAdapter;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.View;
import android.widget.SeekBar;

import com.nagopy.android.overlayviewmanager.OverlayView;
import com.nagopy.android.overlayviewmanager.OverlayViewManager;
import com.nagopy.android.overlayviewmanager.sample.databinding.ActivitySampleOverrideScreenBrightnessBinding;

public class SampleOverrideScreenBrightnessActivity extends BaseSampleWithCodeActivity implements SeekBarBindingAdapter.OnProgressChanged {

    OverlayView<View> overlayView;
    ActivitySampleOverrideScreenBrightnessBinding binding;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        overlayView = OverlayViewManager.getInstance().newOverlayView(new View(this), this)
                .setAlpha(0);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_sample_override_screen_brightness);
        binding.setBrightness(128);
        binding.setOnProgressChanged(this);
        binding.setIsOverrideEnabled(false);
    }

    @Override
    protected void onDestroy() {
        overlayView.hide();
        super.onDestroy();
    }

    public void onClick(View view) {
        if (overlayView.isVisible()) {
            overlayView.hide();
            binding.setIsOverrideEnabled(false);
        } else {
            overlayView.show();
            binding.setIsOverrideEnabled(true);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        overlayView.setScreenBrightness(progress / 255f).update();
        binding.setBrightness(progress);
    }
}
