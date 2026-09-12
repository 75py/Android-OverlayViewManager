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
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
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
        MockitoAnnotations.openMocks(this);

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
        debugOverlayTree.initialize(application);
        debugOverlayTree.overlayView = overlayView;

        debugOverlayTree.setMaxLines(1);

        assertThat(debugOverlayTree.maxLines, is(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void setMaxLines_zero_throws() throws Exception {
        debugOverlayTree.initialize(application);
        debugOverlayTree.overlayView = overlayView;

        debugOverlayTree.setMaxLines(0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void setMaxLines_negative_throws() throws Exception {
        debugOverlayTree.initialize(application);
        debugOverlayTree.overlayView = overlayView;

        debugOverlayTree.setMaxLines(-1);
    }

    @Test
    public void setMaxLines_invalid_doesNotChangeState() throws Exception {
        debugOverlayTree.initialize(application);
        debugOverlayTree.overlayView = overlayView;

        try {
            debugOverlayTree.setMaxLines(0);
            fail("expected IllegalArgumentException");
        } catch (IllegalArgumentException expected) {
            // no-op
        }

        assertThat(debugOverlayTree.maxLines, is(DebugOverlayTree.DEFAULT_MAX_LINES));
    }

    @Test
    public void setMaxLines_shrink_trimsBufferAndRerenders() throws Exception {
        debugOverlayTree.initialize(application);
        debugOverlayTree.overlayView = overlayView;
        for (int i = 1; i <= 5; i++) {
            debugOverlayTree.log(Log.DEBUG, "tag", "message" + i, null);
        }
        reset(textView);

        debugOverlayTree.setMaxLines(2);

        assertThat(debugOverlayTree.messages.size(), is(2));
        verify(textView, times(1)).setText("tag: message4\ntag: message5");
    }

    @Test
    public void setMaxLines_equal_doesNotTrimOrRerender() throws Exception {
        debugOverlayTree.initialize(application);
        debugOverlayTree.overlayView = overlayView;
        for (int i = 1; i <= 5; i++) {
            debugOverlayTree.log(Log.DEBUG, "tag", "message" + i, null);
        }
        reset(textView);

        debugOverlayTree.setMaxLines(5);

        assertThat(debugOverlayTree.messages.size(), is(5));
        verify(textView, never()).setText(anyString());
    }

    @Test
    public void setMaxLines_grow_doesNotTrimExistingBuffer() throws Exception {
        debugOverlayTree.initialize(application);
        debugOverlayTree.overlayView = overlayView;
        for (int i = 1; i <= 5; i++) {
            debugOverlayTree.log(Log.DEBUG, "tag", "message" + i, null);
        }
        reset(textView);

        debugOverlayTree.setMaxLines(10);

        assertThat(debugOverlayTree.maxLines, is(10));
        assertThat(debugOverlayTree.messages.size(), is(5));
        verify(textView, never()).setText(anyString());
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

        verify(textView, never()).setText(anyString());
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
    public void log_rendersOnlyThroughMainThreadPost_neverDirectly() throws Exception {
        debugOverlayTree.initialize(application);
        debugOverlayTree.overlayView = overlayView;
        DebugOverlayTree spied = spy(debugOverlayTree);
        final List<Runnable> posted = Collections.synchronizedList(new ArrayList<Runnable>());
        doAnswer(invocation -> {
            posted.add(invocation.getArgument(0));
            return null;
        }).when(spied).postToMainThread(any(Runnable.class));

        spied.log(Log.DEBUG, "tag", "message", null);

        verify(textView, never()).setText(anyString());
        assertThat(posted.size(), is(1));

        posted.get(0).run();

        verify(textView, times(1)).setText("tag: message");
    }

    @Test
    public void log_highFrequencyLogging_coalescesToSinglePendingRender() throws Exception {
        debugOverlayTree.initialize(application);
        debugOverlayTree.overlayView = overlayView;
        DebugOverlayTree spied = spy(debugOverlayTree);
        final List<Runnable> posted = Collections.synchronizedList(new ArrayList<Runnable>());
        doAnswer(invocation -> {
            posted.add(invocation.getArgument(0));
            return null;
        }).when(spied).postToMainThread(any(Runnable.class));

        for (int i = 0; i < 500; i++) {
            spied.log(Log.DEBUG, "tag", "message" + i, null);
        }

        assertThat("logging must coalesce onto a single pending render", posted.size(), is(1));
        verify(textView, never()).setText(anyString());

        posted.get(0).run();

        verify(textView, times(1)).setText(
                "tag: message495\ntag: message496\ntag: message497\ntag: message498\ntag: message499");
    }

    @Test
    public void log_concurrentThreads_doesNotCorruptBufferOrThrow() throws Exception {
        debugOverlayTree.initialize(application);
        debugOverlayTree.overlayView = overlayView;

        final int threadCount = 8;
        final int perThread = 200;
        final CountDownLatch startLatch = new CountDownLatch(1);
        final CountDownLatch doneLatch = new CountDownLatch(threadCount);
        final List<Throwable> errors = Collections.synchronizedList(new ArrayList<Throwable>());

        for (int t = 0; t < threadCount; t++) {
            final int threadIndex = t;
            Thread thread = new Thread(() -> {
                try {
                    startLatch.await();
                    for (int i = 0; i < perThread; i++) {
                        debugOverlayTree.log(Log.DEBUG, "t" + threadIndex, "m" + i, null);
                    }
                } catch (Throwable e) {
                    errors.add(e);
                } finally {
                    doneLatch.countDown();
                }
            });
            thread.start();
        }
        startLatch.countDown();

        assertThat(doneLatch.await(10, TimeUnit.SECONDS), is(true));
        assertThat(errors.isEmpty(), is(true));
        assertThat(debugOverlayTree.messages.size(), is(debugOverlayTree.maxLines));

        Pattern linePattern = Pattern.compile("^t\\d+: m\\d+$");
        for (String line : debugOverlayTree.messages) {
            assertThat(linePattern.matcher(line).matches(), is(true));
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