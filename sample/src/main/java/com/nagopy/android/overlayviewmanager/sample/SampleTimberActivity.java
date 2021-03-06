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
import androidx.annotation.Nullable;
import android.view.View;

import com.nagopy.android.overlayviewmanager.opt.timber.DebugOverlayTree;

import timber.log.Timber;

public class SampleTimberActivity extends BaseSampleWithCodeActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        DebugOverlayTree.getInstance().register(this);
        setContentView(R.layout.activity_sample_timber);

        Timber.d("onCreate");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Timber.d("onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Timber.d("onStart");
    }

    public void onClick(View view) {
        Timber.d("onClick %d", view.getId());
    }

}
