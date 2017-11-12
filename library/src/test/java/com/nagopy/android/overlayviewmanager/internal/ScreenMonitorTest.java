package com.nagopy.android.overlayviewmanager.internal;

import android.os.Build;
import android.view.WindowManager;

import com.nagopy.android.overlayviewmanager.BuildConfig;
import com.nagopy.android.overlayviewmanager.OverlayView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class
        , sdk = Build.VERSION_CODES.LOLLIPOP
        , manifest = Config.NONE
)
public class ScreenMonitorTest {

    ScreenMonitor screenMonitor;

    @Mock
    ScreenMonitor.ScreenMonitorView monitorView;

    @Mock
    WindowManager.LayoutParams params;

    @Mock
    OverlayWindowManager overlayWindowManager;

    @Mock
    WeakReferenceCache<OverlayView<?>> requestedViews;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        screenMonitor = new ScreenMonitor();
        ScreenMonitor.setInstance(screenMonitor);
        screenMonitor.monitorView = monitorView;
        screenMonitor.params = params;
        screenMonitor.requestedViews = requestedViews;
        screenMonitor.overlayWindowManager = overlayWindowManager;
        OverlayWindowManager.setInstance(overlayWindowManager);
    }

    @Config(sdk = Build.VERSION_CODES.O)
    @Test
    public void init_O() throws Exception {
        screenMonitor.monitorView = null;
        screenMonitor.params = null;
        screenMonitor.requestedViews = null;
        screenMonitor.overlayWindowManager = null;

        ScreenMonitor.init(RuntimeEnvironment.application);

        assertThat(screenMonitor.monitorView, is(notNullValue()));
        assertThat(screenMonitor.params, is(notNullValue()));
        assertThat(screenMonitor.requestedViews, is(notNullValue()));
        assertThat(screenMonitor.overlayWindowManager, is(overlayWindowManager));

        assertThat(screenMonitor.params.width, is(1));
        assertThat(screenMonitor.params.height, is(MATCH_PARENT));
        assertThat(screenMonitor.params.type, is(TYPE_APPLICATION_OVERLAY));
    }

    @Config(sdk = Build.VERSION_CODES.N)
    @Test
    public void init_N() throws Exception {
        screenMonitor.monitorView = null;
        screenMonitor.params = null;
        screenMonitor.requestedViews = null;

        ScreenMonitor.init(RuntimeEnvironment.application);
        assertThat(screenMonitor.monitorView, is(notNullValue()));
        assertThat(screenMonitor.params, is(notNullValue()));
        assertThat(screenMonitor.requestedViews, is(notNullValue()));

        assertThat(screenMonitor.params.width, is(1));
        assertThat(screenMonitor.params.height, is(MATCH_PARENT));
        assertThat(screenMonitor.params.type, is(TYPE_SYSTEM_OVERLAY));
    }

    @Test
    public void request_first() throws Exception {
        OverlayView overlayView1 = mock(OverlayView.class);
        when(requestedViews.isEmpty()).thenReturn(true);
        screenMonitor.request(overlayView1);
        verify(overlayWindowManager, times(1)).show(monitorView, params);
        verify(requestedViews, times(1)).isEmpty();
        verify(requestedViews, times(1)).add(overlayView1);
    }

    @Test
    public void request_second() throws Exception {
        OverlayView overlayView1 = mock(OverlayView.class);
        when(requestedViews.isEmpty()).thenReturn(false);
        screenMonitor.request(overlayView1);
        verify(overlayWindowManager, never()).show(monitorView, params);
        verify(requestedViews, times(1)).isEmpty();
        verify(requestedViews, times(1)).add(overlayView1);
    }

    @Test
    public void cancel() throws Exception {
        OverlayView overlayView1 = mock(OverlayView.class);
        when(requestedViews.isEmpty()).thenReturn(false).thenReturn(true);
        screenMonitor.cancel(overlayView1);
        verify(overlayWindowManager, times(1)).hide(monitorView);
        verify(requestedViews, times(2)).isEmpty();
        verify(requestedViews, times(1)).remove(overlayView1);
    }

    @Test
    public void cancel_doNothing() throws Exception {
        OverlayView overlayView1 = mock(OverlayView.class);
        when(requestedViews.isEmpty()).thenReturn(true);
        screenMonitor.cancel(overlayView1);
        verify(overlayWindowManager, never()).hide(monitorView);
        verify(requestedViews, times(1)).isEmpty();
        verify(requestedViews, never()).remove(overlayView1);
    }

    @Test
    public void getStatusBarHeight() throws Exception {
        screenMonitor.monitorView.statusBarHeight = 84;
        assertThat(screenMonitor.getStatusBarHeight(), is(84));
    }

}