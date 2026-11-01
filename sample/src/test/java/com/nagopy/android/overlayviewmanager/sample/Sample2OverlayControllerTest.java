/*
 * Copyright 2026 75py
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import android.app.Application;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.nagopy.android.overlayviewmanager.OverlayResult;
import com.nagopy.android.overlayviewmanager.OverlayState;
import com.nagopy.android.overlayviewmanager.OverlayView;
import com.nagopy.android.overlayviewmanager.OverlayViewManager;
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowSettings;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Behavior of the application-owned Sample2 handle: a failed disposal keeps the SAME handle and
 * reports it, an explicit retry releases it, and the Activity control is driven by the listener
 * rather than by timing.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.M, application = Application.class)
public class Sample2OverlayControllerTest {

    private Application application;
    private RecordingBackend backend;
    private Sample2OverlayController controller;

    @Before
    public void setUp() throws Exception {
        ShadowSettings.setCanDrawOverlays(true);
        resetOverlayViewManagerSingleton();
        application = RuntimeEnvironment.getApplication();
        OverlayViewManager.init(application);
        backend = new RecordingBackend();
        OverlayWindowManager.setApplicationInstance(backend);
        controller = new Sample2OverlayController();
    }

    @Test
    public void failedDispose_keepsTheSameHandleAttached_thenExplicitRetryReleasesIt() throws Exception {
        List<Boolean> notifications = new ArrayList<>();
        controller.setListener(notifications::add);

        assertTrue(controller.show(application).isSuccess());
        OverlayView<ImageView> handle = reflectHandle();
        assertNotNull(handle);
        assertEquals(OverlayState.ATTACHED, handle.getState());
        assertFalse(controller.hasRetainedFailedDisposal());

        // The window cannot be removed while the Service is being destroyed.
        backend.hideFailure = new RuntimeException("removeViewImmediate failed");
        OverlayResult failed = controller.dispose();

        assertFalse(failed.isSuccess());
        assertEquals(1, backend.hideCalls);
        assertTrue(controller.hasHandle());
        assertTrue(controller.hasRetainedFailedDisposal());
        assertSame(handle, reflectHandle());
        assertEquals(OverlayState.ATTACHED, handle.getState());

        // The user presses the retry control once the platform accepts the removal again.
        backend.hideFailure = null;
        OverlayResult retried = controller.dispose();

        assertTrue(retried.isSuccess());
        assertEquals(2, backend.hideCalls);
        assertFalse(controller.hasHandle());
        assertFalse(controller.hasRetainedFailedDisposal());
        assertEquals(OverlayState.DISPOSED, handle.getState());

        // show -> false, failed dispose -> true, successful retry -> false: the Activity control
        // follows these notifications, not a timer.
        assertEquals(List.of(false, true, false), notifications);
    }

    @Test
    public void dispose_withoutHandle_isSuccessfulNoOp() {
        OverlayResult result = controller.dispose();

        assertTrue(result.isSuccess());
        assertFalse(result.getChanged());
        assertEquals(OverlayState.DISPOSED, result.getState());
        assertFalse(controller.hasRetainedFailedDisposal());
    }

    @Test
    public void show_reusesTheRetainedHandleAfterFailedDispose() throws Exception {
        assertTrue(controller.show(application).isSuccess());
        OverlayView<ImageView> handle = reflectHandle();
        backend.hideFailure = new RuntimeException("boom");
        assertFalse(controller.dispose().isSuccess());

        // Starting the Service again must not create a second window for the same overlay.
        OverlayResult shownAgain = controller.show(application);

        assertTrue(shownAgain.isSuccess());
        assertFalse(shownAgain.getChanged());
        assertSame(handle, reflectHandle());
        assertFalse(controller.hasRetainedFailedDisposal());
    }

    @SuppressWarnings("unchecked")
    @Nullable
    private OverlayView<ImageView> reflectHandle() throws Exception {
        Field field = Sample2OverlayController.class.getDeclaredField("overlayView");
        field.setAccessible(true);
        return (OverlayView<ImageView>) field.get(controller);
    }

    private static void resetOverlayViewManagerSingleton() throws Exception {
        Field field = OverlayViewManager.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
    }

    /** Test backend: records removals and can be told to fail them, like a rejected removeViewImmediate. */
    private static final class RecordingBackend extends OverlayWindowManager {
        @Nullable
        RuntimeException hideFailure;
        int hideCalls;

        @Override
        public void show(@NonNull View view, @NonNull WindowManager.LayoutParams params) {
        }

        @Override
        public void update(@NonNull View view, @NonNull WindowManager.LayoutParams params) {
        }

        @Override
        public void hide(@NonNull View view) {
            hideCalls++;
            if (hideFailure != null) {
                throw hideFailure;
            }
        }
    }
}
