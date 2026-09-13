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

import android.content.Context;
import android.content.Intent;
import android.widget.ImageView;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import com.nagopy.android.overlayviewmanager.OverlayResult;
import com.nagopy.android.overlayviewmanager.OverlaySpec;
import com.nagopy.android.overlayviewmanager.OverlayState;
import com.nagopy.android.overlayviewmanager.OverlayTouchMode;
import com.nagopy.android.overlayviewmanager.OverlayView;
import com.nagopy.android.overlayviewmanager.OverlayViewManager;

/**
 * Application-owned holder of the Sample2 overlay handle. The handle outlives
 * {@link Sample2Activity.Sample2Service}: when the Service is destroyed while the window cannot be
 * removed, the sole handle is retained here so the user can retry {@link #dispose()} later from
 * {@link Sample2Activity}. Nothing retries automatically. All methods are main-thread only.
 */
final class Sample2OverlayController {

    private static final Sample2OverlayController INSTANCE = new Sample2OverlayController();

    static Sample2OverlayController get() {
        return INSTANCE;
    }

    @Nullable
    private OverlayView<ImageView> overlayView;

    private Sample2OverlayController() {
    }

    /** Creates the handle on first use with the application context and shows it. */
    @MainThread
    OverlayResult show(Context context) {
        OverlayView<ImageView> handle = overlayView;
        if (handle == null) {
            Context appContext = context.getApplicationContext();
            ImageView imageView = new ImageView(appContext);
            imageView.setId(R.id.sample_image_view);
            imageView.setImageResource(R.mipmap.ic_launcher);
            // Ordinary click delivery stays on the View itself, even while DRAGGABLE.
            imageView.setOnClickListener(v -> appContext.startActivity(
                    new Intent(appContext, Sample2Activity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)));
            handle = OverlayViewManager.getInstance().newOverlayView(
                    imageView,
                    new OverlaySpec.Builder().setTouchMode(OverlayTouchMode.DRAGGABLE).build());
            overlayView = handle;
        }
        return handle.show();
    }

    /**
     * Disposes the handle. On failure the handle is kept, still attached, so that a later explicit
     * call can retry; only a successful disposal drops it.
     */
    @MainThread
    OverlayResult dispose() {
        OverlayView<ImageView> handle = overlayView;
        if (handle == null) {
            return new OverlayResult(OverlayState.DISPOSED, false, null, null);
        }
        OverlayResult result = handle.dispose();
        if (result.isSuccess()) {
            overlayView = null;
        }
        return result;
    }

    /** True while a handle exists, including one retained after a failed {@link #dispose()}. */
    boolean hasHandle() {
        return overlayView != null;
    }
}
