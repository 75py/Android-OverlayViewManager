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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
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
    WeakReferenceCache<Activity> registeredActivitiesMock;

    @Mock
    WeakReferenceCache<Activity> runningActivitiesMock;

    @Mock
    WeakReferenceCache<Activity> registeredAndRunningActivitiesMock;

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
    public void register_notRunning() throws Exception {
        debugOverlayTree.registeredActivities = registeredActivitiesMock;
        debugOverlayTree.runningActivities = runningActivitiesMock;
        debugOverlayTree.registeredAndRunningActivities = registeredAndRunningActivitiesMock;
        when(runningActivitiesMock.contains(activity)).thenReturn(false);

        debugOverlayTree.register(activity);

        verify(registeredActivitiesMock, times(1)).add(activity);
        verify(runningActivitiesMock, times(1)).contains(activity);
        verify(registeredAndRunningActivitiesMock, never()).add(activity);
        verify(overlayView, never()).show();
    }

    @Test
    public void register_running() throws Exception {
        debugOverlayTree.overlayView = overlayView;
        debugOverlayTree.registeredActivities = registeredActivitiesMock;
        debugOverlayTree.runningActivities = runningActivitiesMock;
        debugOverlayTree.registeredAndRunningActivities = registeredAndRunningActivitiesMock;
        when(runningActivitiesMock.contains(activity)).thenReturn(true);

        debugOverlayTree.register(activity);

        verify(registeredActivitiesMock, times(1)).add(activity);
        verify(runningActivitiesMock, times(1)).contains(activity);
        verify(registeredAndRunningActivitiesMock, times(1)).add(activity);
        verify(overlayView, times(1)).show();
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
        debugOverlayTree.registeredActivities = registeredActivitiesMock;
        debugOverlayTree.runningActivities = runningActivitiesMock;
        debugOverlayTree.registeredAndRunningActivities = registeredAndRunningActivitiesMock;
        debugOverlayTree.overlayView = overlayView;
        {
            when(registeredActivitiesMock.contains(activity)).thenReturn(false);

            debugOverlayTree.activityLifecycleCallbacks.onActivityStarted(activity);

            verify(registeredAndRunningActivitiesMock, never()).add(activity);
            verify(overlayView, never()).show();
        }
        reset(overlayView, registeredActivitiesMock, runningActivitiesMock, registeredAndRunningActivitiesMock);
        {
            when(registeredActivitiesMock.contains(activity)).thenReturn(true);

            debugOverlayTree.activityLifecycleCallbacks.onActivityStarted(activity);

            verify(runningActivitiesMock, times(1)).add(activity);
            verify(overlayView, times(1)).show();
        }
    }

    @Test
    public void SimpleActivityLifecycleCallbacks_onActivityStopped() throws Exception {
        debugOverlayTree.overlayView = overlayView;
        debugOverlayTree.registeredActivities = registeredActivitiesMock;
        debugOverlayTree.runningActivities = runningActivitiesMock;
        debugOverlayTree.registeredAndRunningActivities = registeredAndRunningActivitiesMock;
        {
            when(registeredActivitiesMock.contains(activity)).thenReturn(false);
            when(registeredActivitiesMock.isEmpty()).thenReturn(false);

            debugOverlayTree.activityLifecycleCallbacks.onActivityStopped(activity);

            verify(registeredAndRunningActivitiesMock, never()).remove(activity);
            verify(overlayView, never()).hide();
            verify(runningActivitiesMock, times(1)).remove(activity);
        }
        {
            reset(overlayView, registeredActivitiesMock, runningActivitiesMock, registeredAndRunningActivitiesMock);
            {
                when(registeredActivitiesMock.contains(activity)).thenReturn(true);
                when(registeredActivitiesMock.isEmpty()).thenReturn(false);
                when(registeredAndRunningActivitiesMock.isEmpty()).thenReturn(false);

                debugOverlayTree.activityLifecycleCallbacks.onActivityStopped(activity);

                verify(registeredAndRunningActivitiesMock, times(1)).isEmpty();
                verify(overlayView, never()).hide();
                verify(runningActivitiesMock, times(1)).remove(activity);
            }
            reset(overlayView, registeredActivitiesMock, runningActivitiesMock, registeredAndRunningActivitiesMock);
            {
                when(registeredActivitiesMock.contains(activity)).thenReturn(true);
                when(registeredActivitiesMock.isEmpty()).thenReturn(false);
                when(registeredAndRunningActivitiesMock.isEmpty()).thenReturn(true);

                debugOverlayTree.activityLifecycleCallbacks.onActivityStopped(activity);

                verify(registeredAndRunningActivitiesMock, times(1)).isEmpty();
                verify(overlayView, times(1)).hide();
                verify(runningActivitiesMock, times(1)).remove(activity);
            }
            reset(overlayView, registeredActivitiesMock, runningActivitiesMock, registeredAndRunningActivitiesMock);
            {
                when(registeredActivitiesMock.contains(activity)).thenReturn(false);
                when(registeredActivitiesMock.isEmpty()).thenReturn(true);
                when(registeredAndRunningActivitiesMock.isEmpty()).thenReturn(false);

                debugOverlayTree.activityLifecycleCallbacks.onActivityStopped(activity);

                verify(registeredAndRunningActivitiesMock, times(1)).isEmpty();
                verify(overlayView, never()).hide();
                verify(runningActivitiesMock, times(1)).remove(activity);
            }
            reset(overlayView, registeredActivitiesMock, runningActivitiesMock, registeredAndRunningActivitiesMock);
            {
                when(registeredActivitiesMock.contains(activity)).thenReturn(false);
                when(registeredActivitiesMock.isEmpty()).thenReturn(true);
                when(registeredAndRunningActivitiesMock.isEmpty()).thenReturn(true);

                debugOverlayTree.activityLifecycleCallbacks.onActivityStopped(activity);

                verify(registeredAndRunningActivitiesMock, times(1)).isEmpty();
                verify(overlayView, times(1)).hide();
                verify(runningActivitiesMock, times(1)).remove(activity);
            }
            reset(overlayView, registeredActivitiesMock, runningActivitiesMock, registeredAndRunningActivitiesMock);
        }
    }

    @Test
    public void SimpleActivityLifecycleCallbacks_onActivityDestroyed() throws Exception {
        {
            debugOverlayTree.registeredActivities = registeredActivitiesMock;

            debugOverlayTree.activityLifecycleCallbacks.onActivityDestroyed(activity);

            verify(registeredActivitiesMock, times(1)).remove(activity);
        }
    }

}