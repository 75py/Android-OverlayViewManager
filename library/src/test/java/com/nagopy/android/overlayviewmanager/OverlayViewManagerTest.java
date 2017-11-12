package com.nagopy.android.overlayviewmanager;

import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class
        , sdk = Build.VERSION_CODES.LOLLIPOP
        , manifest = Config.NONE
)
public class OverlayViewManagerTest {

    @Mock
    Context context;

    @Mock
    WindowManager windowManager;

    @Mock
    View view;
    @Mock
    View view2;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        OverlayViewManager.applicationContext = null;

        assertThat(context, is(notNullValue()));
        Mockito.when(context.getApplicationContext()).thenReturn(RuntimeEnvironment.application);
        Mockito.when(context.getSystemService(Context.WINDOW_SERVICE)).thenReturn(windowManager);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void init() throws Exception {
        OverlayViewManager.init(context);
        assertThat(OverlayViewManager.applicationContext, is((Context) RuntimeEnvironment.application));
    }

    @Config(sdk = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Test
    public void canDrawOverlays_22() throws Exception {
        assertTrue(OverlayViewManager.canDrawOverlays());
    }

    /*
    @Config(sdk = Build.VERSION_CODES.M)
    @Test
    public void canDrawOverlays_23() throws Exception {
        OverlayViewManager.applicationContext = applicationContext;
        assertTrue(OverlayViewManager.canDrawOverlays());
    }
    */

}