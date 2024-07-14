package com.nagopy.android.overlayviewmanager;

import android.app.Activity;
import android.app.Application;
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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nagopy.android.overlayviewmanager.internal.ScreenMonitor;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.LOLLIPOP
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

    @Mock
    ScreenMonitor screenMonitor;

    OverlayViewManager overlayViewManager;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);

        ScreenMonitor.setInstance(screenMonitor);

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

        assertThat(overlayViewManager.applicationContext, is(equalTo(application.getApplicationContext())));
        assertThat(overlayViewManager.windowManager, is(notNullValue()));
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

    @Test
    public void newOverlayView() throws Exception {
        OverlayView<View> overlayView = overlayViewManager.newOverlayView(view);
        assertThat(overlayView.getView(), is(view));
        assertThat(overlayView.viewScope, is(OverlayView.ViewScope.APPLICATION));

        Activity activity = mock(Activity.class);
        OverlayView<View> overlayView2 = overlayViewManager.newOverlayView(view2, activity);
        assertThat(overlayView2.getView(), is(view2));
        assertThat(overlayView2.viewScope, is(OverlayView.ViewScope.ACTIVITY));
    }

}