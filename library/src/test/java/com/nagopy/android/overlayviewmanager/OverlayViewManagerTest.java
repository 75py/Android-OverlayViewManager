package com.nagopy.android.overlayviewmanager;

import android.app.Application;
import android.content.Context;
import android.os.Build;
import android.view.View;
import android.view.WindowManager;

import org.hamcrest.Matchers;
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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class
        , sdk = Build.VERSION_CODES.LOLLIPOP
        , manifest = Config.NONE
)
public class OverlayViewManagerTest {

    @Mock
    Application application;

    @Mock
    WindowManager windowManager;

    @Mock
    View view;
    @Mock
    View view2;

    OverlayViewManager overlayViewManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        application = Mockito.spy(RuntimeEnvironment.application);
        when(application.getSystemService(Context.WINDOW_SERVICE)).thenReturn(windowManager);
        when(application.getApplicationContext()).thenReturn(application);
        OverlayViewManager.init(application);

        overlayViewManager = new OverlayViewManager();
        OverlayViewManager.INSTANCE = overlayViewManager;
    }

    @After
    public void tearDown() throws Exception {
    }


    @Test(expected = IllegalStateException.class)
    public void getInstance_noContext() throws Exception {
        OverlayViewManager.INSTANCE.applicationContext = null;
        OverlayViewManager.getInstance();
    }

    @Test
    public void getInstance() throws Exception {
        OverlayViewManager.INSTANCE.applicationContext = application;
        OverlayViewManager.getInstance();
    }

    @Test
    public void init() throws Exception {
        OverlayViewManager mock = mock(OverlayViewManager.class);
        OverlayViewManager.INSTANCE = mock;
        OverlayViewManager.init(application);

        verify(mock, times(1)).initialize(application);
    }

    @Test
    public void initialize() throws Exception {
        overlayViewManager.initialize(application);

        assertThat(overlayViewManager.applicationContext, Matchers.is(equalTo(application.getApplicationContext())));
        assertThat(overlayViewManager.windowManager, Matchers.is(Matchers.notNullValue()));
    }

    @Config(sdk = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Test
    public void canDrawOverlays_22() throws Exception {
        OverlayViewManager.init(application);

        assertTrue(OverlayViewManager.getInstance().canDrawOverlays());
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