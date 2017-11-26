package com.nagopy.android.overlayviewmanager.opt.timber;

import android.app.Activity;
import android.app.Application;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;

import com.nagopy.android.overlayviewmanager.OverlayView;
import com.nagopy.android.overlayviewmanager.OverlayViewManager;
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowManager;
import com.nagopy.android.overlayviewmanager.internal.WeakReferenceCache;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DebugOverlayTreeTest {

    DebugOverlayTree debugOverlayTree;

    @Mock
    OverlayWindowManager overlayWindowManager;

    @Mock
    WindowManager windowManager;

    @Mock
    Application application;

    @Mock
    Activity activity;

    @Mock
    WeakReferenceCache<Activity> weakReferenceCache;

    @Mock
    OverlayView<TextView> overlayView;

    @Mock
    TextView textView;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        when(application.getApplicationContext()).thenReturn(application);
        OverlayViewManager.init(application);
        OverlayWindowManager.setApplicationInstance(overlayWindowManager);
        OverlayWindowManager.initApplicationInstance(windowManager);


        debugOverlayTree = new DebugOverlayTree();
        assertThat(debugOverlayTree.overlayView, is(nullValue()));

        when(overlayView.getView()).thenReturn(textView);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test(expected = IllegalStateException.class)
    public void getInstance_noView() throws Exception {
        DebugOverlayTree.INSTANCE.overlayView = null;
        DebugOverlayTree.getInstance();
    }

    @Test
    public void getInstance() throws Exception {
        DebugOverlayTree.INSTANCE.overlayView = overlayView;
        DebugOverlayTree.getInstance();
    }

    @Test
    public void init() throws Exception {
        DebugOverlayTree mock = mock(DebugOverlayTree.class);
        DebugOverlayTree.INSTANCE = mock;
        DebugOverlayTree.init(application);

        verify(mock, times(1)).initialize(application);
    }

    @Test
    public void initialize() throws Exception {
        debugOverlayTree.initialize(application);

        assertThat(debugOverlayTree.messages, is(notNullValue()));
        assertThat(debugOverlayTree.threshold, is(Log.DEBUG));
        assertThat(debugOverlayTree.maxLines, is(5));
        assertThat(debugOverlayTree.overlayView, is(notNullValue()));
        assertThat(debugOverlayTree.registeredActivities, is(notNullValue()));
        verify(application, times(1)).registerActivityLifecycleCallbacks(debugOverlayTree.activityLifecycleCallbacks);
    }

    @Test
    public void setThreshold() throws Exception {
        debugOverlayTree.threshold = 0;

        debugOverlayTree.setThreshold(1);
        assertThat(debugOverlayTree.threshold, is(1));
    }

    @Test
    public void setMaxLines() throws Exception {
        debugOverlayTree.maxLines = 0;

        debugOverlayTree.setMaxLines(1);
        assertThat(debugOverlayTree.maxLines, is(1));
    }

    @Test
    public void register() throws Exception {
        debugOverlayTree.registeredActivities = weakReferenceCache;
        debugOverlayTree.register(activity);

        verify(weakReferenceCache, times(1)).add(activity);
    }

    @Test
    public void log_threshold() throws Exception {
        debugOverlayTree.initialize(application);
        debugOverlayTree.overlayView = overlayView;

        debugOverlayTree.log(Log.VERBOSE, "tag", "message", null);

        verify(textView, never()).setText(Mockito.anyString());
    }

    @Test
    public void log() throws Exception {
        debugOverlayTree.initialize(application);
        debugOverlayTree.overlayView = overlayView;

        debugOverlayTree.log(Log.DEBUG, "tag", "message", null);

        verify(textView, times(1)).setText("tag: message");
    }

    @Test
    public void log_multiLine() throws Exception {
        debugOverlayTree.initialize(application);
        debugOverlayTree.overlayView = overlayView;
        debugOverlayTree.messages.add("tag: message");

        debugOverlayTree.log(Log.DEBUG, "tag", "message2", null);

        verify(textView, times(1)).setText("tag: message\ntag: message2");
    }

    @Test
    public void log_multiLine_max() throws Exception {
        debugOverlayTree.initialize(application);
        debugOverlayTree.overlayView = overlayView;
        debugOverlayTree.messages.add("tag: message1");
        debugOverlayTree.messages.add("tag: message2");
        debugOverlayTree.messages.add("tag: message3");
        debugOverlayTree.messages.add("tag: message4");
        debugOverlayTree.messages.add("tag: message5");

        debugOverlayTree.log(Log.DEBUG, "tag", "message6", null);

        verify(textView, times(1)).setText("tag: message2\n" +
                "tag: message3\n" +
                "tag: message4\n" +
                "tag: message5\n" +
                "tag: message6");
    }

    @Test
    public void SimpleActivityLifecycleCallbacks_onActivityStarted() throws Exception {
        {
            debugOverlayTree.registeredActivities = weakReferenceCache;
            debugOverlayTree.overlayView = overlayView;
            when(weakReferenceCache.contains(activity)).thenReturn(false);

            debugOverlayTree.activityLifecycleCallbacks.onActivityStarted(activity);

            verify(overlayView, never()).show();
        }
        {
            debugOverlayTree.registeredActivities = weakReferenceCache;
            debugOverlayTree.overlayView = overlayView;
            when(weakReferenceCache.contains(activity)).thenReturn(true);

            debugOverlayTree.activityLifecycleCallbacks.onActivityStarted(activity);

            verify(overlayView, times(1)).show();
        }
    }

    @Test
    public void SimpleActivityLifecycleCallbacks_onActivityStopped() throws Exception {
        {
            debugOverlayTree.registeredActivities = weakReferenceCache;
            debugOverlayTree.overlayView = overlayView;
            when(weakReferenceCache.contains(activity)).thenReturn(false);

            debugOverlayTree.activityLifecycleCallbacks.onActivityStopped(activity);

            verify(overlayView, never()).hide();
        }
        {
            {
                debugOverlayTree.registeredActivities = weakReferenceCache;
                debugOverlayTree.overlayView = overlayView;
                debugOverlayTree.activityLifecycleCallbacks.startedCount = 2;
                when(weakReferenceCache.contains(activity)).thenReturn(true);
                when(weakReferenceCache.isEmpty()).thenReturn(false);

                debugOverlayTree.activityLifecycleCallbacks.onActivityStopped(activity);

                assertThat(debugOverlayTree.activityLifecycleCallbacks.startedCount, is(1));
                verify(overlayView, never()).hide();
            }
            {
                debugOverlayTree.registeredActivities = weakReferenceCache;
                debugOverlayTree.overlayView = overlayView;
                debugOverlayTree.activityLifecycleCallbacks.startedCount = 2;
                when(weakReferenceCache.contains(activity)).thenReturn(false);
                when(weakReferenceCache.isEmpty()).thenReturn(true);

                debugOverlayTree.activityLifecycleCallbacks.onActivityStopped(activity);

                assertThat(debugOverlayTree.activityLifecycleCallbacks.startedCount, is(1));
                verify(overlayView, never()).hide();
            }
            {
                debugOverlayTree.registeredActivities = weakReferenceCache;
                debugOverlayTree.overlayView = overlayView;
                debugOverlayTree.activityLifecycleCallbacks.startedCount = 1;
                when(weakReferenceCache.contains(activity)).thenReturn(true);
                when(weakReferenceCache.isEmpty()).thenReturn(true);

                debugOverlayTree.activityLifecycleCallbacks.onActivityStopped(activity);

                assertThat(debugOverlayTree.activityLifecycleCallbacks.startedCount, is(0));
                verify(overlayView, times(1)).hide();
            }
        }
    }

    @Test
    public void SimpleActivityLifecycleCallbacks_onActivityDestroyed() throws Exception {
        {
            debugOverlayTree.registeredActivities = weakReferenceCache;

            debugOverlayTree.activityLifecycleCallbacks.onActivityDestroyed(activity);

            verify(weakReferenceCache, times(1)).remove(activity);
        }
    }

}