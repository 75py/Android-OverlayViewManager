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

package com.nagopy.android.overlayviewmanager.opt.timber;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.util.Log;
import android.view.WindowManager;

import com.nagopy.android.overlayviewmanager.OverlayResult;
import com.nagopy.android.overlayviewmanager.OverlayState;
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

/**
 * Exercises DebugOverlayTree's public surface the way a Java Timber consumer would -- init,
 * getInstance, setThreshold, setMaxLines, register and dispose -- with no Kotlin-specific call
 * syntax (default arguments, named parameters, extension receivers). This is the L6 "Java API
 * compatibility" coverage required by the FROZEN T07 LIFECYCLE CONTRACT.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.M, manifest = Config.NONE)
public class DebugOverlayTreeJavaConsumerTest {

    @Before
    public void setUp() throws Exception {
        ShadowSettings.setCanDrawOverlays(true);
        resetOverlayViewManagerSingleton();
        resetDebugOverlayTreeSingleton();
    }

    @Test
    public void initGetInstanceSetThresholdSetMaxLinesRegisterAndDispose_areCallableFromJava() throws Exception {
        Application application = spy(RuntimeEnvironment.getApplication());
        when(application.getApplicationContext()).thenReturn(application);
        OverlayViewManager.init(application);
        OverlayWindowManager.initApplicationInstance(mock(WindowManager.class));

        DebugOverlayTree tree = DebugOverlayTree.init(application);
        assertNotNull(tree);
        assertSame(tree, DebugOverlayTree.getInstance());

        tree.setThreshold(Log.WARN);
        tree.setMaxLines(3);

        Activity activity = mock(Activity.class);
        tree.register(activity);

        OverlayResult disposeResult = tree.dispose();
        assertTrue(disposeResult.isSuccess());
        assertEquals(OverlayState.DISPOSED, disposeResult.getState());

        assertThrows(IllegalStateException.class, DebugOverlayTree::getInstance);

        // A repeated dispose after success is a no-op, never an exception.
        OverlayResult secondDispose = tree.dispose();
        assertTrue(secondDispose.isSuccess());
        assertFalse(secondDispose.getChanged());

        // Re-init after dispose works cleanly, matching the same Java call pattern as the first init.
        DebugOverlayTree reInitialized = DebugOverlayTree.init(application);
        assertSame(tree, reInitialized);
        assertSame(reInitialized, DebugOverlayTree.getInstance());
        assertTrue(reInitialized.dispose().isSuccess());
    }

    private void resetOverlayViewManagerSingleton() throws Exception {
        Field field = OverlayViewManager.class.getDeclaredField("instance");
        field.setAccessible(true);
        field.set(null, null);
    }

    private void resetDebugOverlayTreeSingleton() throws Exception {
        Field field = DebugOverlayTree.class.getDeclaredField("INSTANCE");
        field.setAccessible(true);
        java.lang.reflect.Constructor<DebugOverlayTree> constructor = DebugOverlayTree.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        field.set(null, constructor.newInstance());
    }
}
