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

import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.nagopy.android.overlayviewmanager.OverlayResult;
import com.nagopy.android.overlayviewmanager.OverlaySpec;
import com.nagopy.android.overlayviewmanager.OverlayState;
import com.nagopy.android.overlayviewmanager.OverlayView;
import com.nagopy.android.overlayviewmanager.OverlayViewManager;
import com.nagopy.android.overlayviewmanager.sample.databinding.ActivitySampleOverrideScreenBrightnessBinding;

import timber.log.Timber;

/**
 * Activity-scoped, fully transparent overlay whose only job is to carry
 * {@link OverlaySpec#getScreenBrightness()}. Brightness override is available for activity-scoped
 * overlays only; application-scoped specs reject it.
 */
public class SampleOverrideScreenBrightnessActivity extends BaseSampleWithCodeActivity implements SeekBar.OnSeekBarChangeListener {

    private static final int INITIAL_BRIGHTNESS = 128;

    OverlayView<View> overlayView;
    ActivitySampleOverrideScreenBrightnessBinding binding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        overlayView = OverlayViewManager.getInstance().newOverlayView(
                new View(this),
                this,
                new OverlaySpec.Builder()
                        .setAlpha(0f)
                        .setScreenBrightness(INITIAL_BRIGHTNESS / 255f)
                        .build());
        binding = DataBindingUtil.setContentView(this, R.layout.activity_sample_override_screen_brightness);
        binding.setBrightness(INITIAL_BRIGHTNESS);
        binding.setOnProgressChanged(this);
        binding.setIsOverrideEnabled(false);
    }

    @Override
    protected void onDestroy() {
        OverlayResult disposed = overlayView.dispose();
        if (!disposed.isSuccess()) {
            Timber.w(disposed.getCause(), "dispose() failed: %s", disposed.getFailure());
        }
        super.onDestroy();
    }

    public void onClick(View view) {
        OverlayResult result = overlayView.getState() == OverlayState.ATTACHED
                ? overlayView.hide()
                : overlayView.show();
        if (!result.isSuccess()) {
            Timber.w(result.getCause(), "show()/hide() failed: %s", result.getFailure());
        }
        binding.setIsOverrideEnabled(overlayView.getState() == OverlayState.ATTACHED);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        // Start from the effective spec and change only the requested field.
        OverlaySpec next = overlayView.getSpec().toBuilder()
                .setScreenBrightness(progress / 255f)
                .build();
        OverlayResult updated = overlayView.update(next);
        if (!updated.isSuccess()) {
            Timber.w(updated.getCause(), "update() failed: %s", updated.getFailure());
        }
        binding.setBrightness(progress);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }
}
