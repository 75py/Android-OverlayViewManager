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

import android.app.Application;

import com.nagopy.android.overlayviewmanager.OverlayViewManager;
import com.nagopy.android.overlayviewmanager.opt.timber.DebugOverlayTree;

import timber.log.Timber;

public class SampleApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        OverlayViewManager.init(this);

        Timber.plant(new Timber.DebugTree());
        Timber.plant(DebugOverlayTree.init(this));
    }

}
