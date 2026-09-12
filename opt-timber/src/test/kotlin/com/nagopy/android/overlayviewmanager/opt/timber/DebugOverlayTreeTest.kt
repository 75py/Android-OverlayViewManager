package com.nagopy.android.overlayviewmanager.opt.timber

import android.app.Activity
import android.app.Application
import android.util.Log
import android.view.WindowManager
import android.widget.TextView
import com.nagopy.android.overlayviewmanager.OverlayView
import com.nagopy.android.overlayviewmanager.OverlayViewManager
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowManager
import com.nagopy.android.overlayviewmanager.internal.WeakReferenceCache
import org.hamcrest.CoreMatchers.`is` as isEqualTo
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenever
import org.mockito.MockitoAnnotations
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

class DebugOverlayTreeTest {

    private lateinit var debugOverlayTree: DebugOverlayTree

    @Mock
    private lateinit var overlayWindowManager: OverlayWindowManager

    @Mock
    private lateinit var windowManager: WindowManager

    @Mock
    private lateinit var application: Application

    @Mock
    private lateinit var activity: Activity

    @Mock
    private lateinit var registeredActivitiesMock: WeakReferenceCache<Activity>

    @Mock
    private lateinit var runningActivitiesMock: WeakReferenceCache<Activity>

    @Mock
    private lateinit var registeredAndRunningActivitiesMock: WeakReferenceCache<Activity>

    @Mock
    private lateinit var overlayView: OverlayView<TextView>

    @Mock
    private lateinit var textView: TextView

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        whenever(application.getApplicationContext()).thenReturn(application)
        OverlayViewManager.init(application)
        OverlayWindowManager.setApplicationInstance(overlayWindowManager)
        OverlayWindowManager.initApplicationInstance(windowManager)

        debugOverlayTree = DebugOverlayTree()
        assertThat(debugOverlayTree.overlayView, isEqualTo(nullValue()))

        whenever(overlayView.getView()).thenReturn(textView)
    }

    @After
    fun tearDown() {
    }

    @Test(expected = IllegalStateException::class)
    fun getInstance_noView() {
        DebugOverlayTree.INSTANCE.overlayView = null
        DebugOverlayTree.getInstance()
    }

    @Test
    fun getInstance() {
        DebugOverlayTree.INSTANCE.overlayView = overlayView
        DebugOverlayTree.getInstance()
    }

    @Test
    fun init() {
        val mockTree = mock(DebugOverlayTree::class.java)
        DebugOverlayTree.INSTANCE = mockTree
        DebugOverlayTree.init(application)

        verify(mockTree, times(1)).initialize(application)
    }

    @Test
    fun initialize() {
        debugOverlayTree.initialize(application)

        assertThat(debugOverlayTree.messages, isEqualTo(notNullValue()))
        assertThat(debugOverlayTree.threshold, isEqualTo(Log.DEBUG))
        assertThat(debugOverlayTree.maxLines, isEqualTo(5))
        assertThat(debugOverlayTree.overlayView, isEqualTo(notNullValue()))
        assertThat(debugOverlayTree.registeredActivities, isEqualTo(notNullValue()))
        verify(application, times(1)).registerActivityLifecycleCallbacks(debugOverlayTree.activityLifecycleCallbacks)
    }

    @Test
    fun setThreshold() {
        debugOverlayTree.threshold = 0

        debugOverlayTree.setThreshold(1)
        assertThat(debugOverlayTree.threshold, isEqualTo(1))
    }

    @Test
    fun setMaxLines() {
        debugOverlayTree.initialize(application)
        debugOverlayTree.overlayView = overlayView

        debugOverlayTree.setMaxLines(1)

        assertThat(debugOverlayTree.maxLines, isEqualTo(1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun setMaxLines_zero_throws() {
        debugOverlayTree.initialize(application)
        debugOverlayTree.overlayView = overlayView

        debugOverlayTree.setMaxLines(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun setMaxLines_negative_throws() {
        debugOverlayTree.initialize(application)
        debugOverlayTree.overlayView = overlayView

        debugOverlayTree.setMaxLines(-1)
    }

    @Test
    fun setMaxLines_invalid_doesNotChangeState() {
        debugOverlayTree.initialize(application)
        debugOverlayTree.overlayView = overlayView

        try {
            debugOverlayTree.setMaxLines(0)
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // no-op
        }

        assertThat(debugOverlayTree.maxLines, isEqualTo(DebugOverlayTree.DEFAULT_MAX_LINES))
    }

    @Test
    fun setMaxLines_shrink_trimsBufferAndRerenders() {
        debugOverlayTree.initialize(application)
        debugOverlayTree.overlayView = overlayView
        for (i in 1..5) {
            debugOverlayTree.logForTest(Log.DEBUG, "tag", "message$i", null)
        }
        reset(textView)

        debugOverlayTree.setMaxLines(2)

        assertThat(debugOverlayTree.messages!!.size, isEqualTo(2))
        verify(textView, times(1)).setText("tag: message4\ntag: message5")
    }

    @Test
    fun setMaxLines_equal_doesNotTrimOrRerender() {
        debugOverlayTree.initialize(application)
        debugOverlayTree.overlayView = overlayView
        for (i in 1..5) {
            debugOverlayTree.logForTest(Log.DEBUG, "tag", "message$i", null)
        }
        reset(textView)

        debugOverlayTree.setMaxLines(5)

        assertThat(debugOverlayTree.messages!!.size, isEqualTo(5))
        verify(textView, never()).setText(anyString())
    }

    @Test
    fun setMaxLines_grow_doesNotTrimExistingBuffer() {
        debugOverlayTree.initialize(application)
        debugOverlayTree.overlayView = overlayView
        for (i in 1..5) {
            debugOverlayTree.logForTest(Log.DEBUG, "tag", "message$i", null)
        }
        reset(textView)

        debugOverlayTree.setMaxLines(10)

        assertThat(debugOverlayTree.maxLines, isEqualTo(10))
        assertThat(debugOverlayTree.messages!!.size, isEqualTo(5))
        verify(textView, never()).setText(anyString())
    }

    @Test
    fun register_notRunning() {
        debugOverlayTree.registeredActivities = registeredActivitiesMock
        debugOverlayTree.runningActivities = runningActivitiesMock
        debugOverlayTree.registeredAndRunningActivities = registeredAndRunningActivitiesMock
        whenever(runningActivitiesMock.contains(activity)).thenReturn(false)

        debugOverlayTree.register(activity)

        verify(registeredActivitiesMock, times(1)).add(activity)
        verify(runningActivitiesMock, times(1)).contains(activity)
        verify(registeredAndRunningActivitiesMock, never()).add(activity)
        verify(overlayView, never()).show()
    }

    @Test
    fun register_running() {
        debugOverlayTree.overlayView = overlayView
        debugOverlayTree.registeredActivities = registeredActivitiesMock
        debugOverlayTree.runningActivities = runningActivitiesMock
        debugOverlayTree.registeredAndRunningActivities = registeredAndRunningActivitiesMock
        whenever(runningActivitiesMock.contains(activity)).thenReturn(true)

        debugOverlayTree.register(activity)

        verify(registeredActivitiesMock, times(1)).add(activity)
        verify(runningActivitiesMock, times(1)).contains(activity)
        verify(registeredAndRunningActivitiesMock, times(1)).add(activity)
        verify(overlayView, times(1)).show()
    }

    @Test
    fun log_threshold() {
        debugOverlayTree.initialize(application)
        debugOverlayTree.overlayView = overlayView

        debugOverlayTree.logForTest(Log.VERBOSE, "tag", "message", null)

        verify(textView, never()).setText(anyString())
    }

    @Test
    fun log() {
        debugOverlayTree.initialize(application)
        debugOverlayTree.overlayView = overlayView

        debugOverlayTree.logForTest(Log.DEBUG, "tag", "message", null)

        verify(textView, times(1)).setText("tag: message")
    }

    @Test
    fun log_multiLine() {
        debugOverlayTree.initialize(application)
        debugOverlayTree.overlayView = overlayView
        debugOverlayTree.messages!!.add("tag: message")

        debugOverlayTree.logForTest(Log.DEBUG, "tag", "message2", null)

        verify(textView, times(1)).setText("tag: message\ntag: message2")
    }

    @Test
    fun log_multiLine_max() {
        debugOverlayTree.initialize(application)
        debugOverlayTree.overlayView = overlayView
        debugOverlayTree.messages!!.add("tag: message1")
        debugOverlayTree.messages!!.add("tag: message2")
        debugOverlayTree.messages!!.add("tag: message3")
        debugOverlayTree.messages!!.add("tag: message4")
        debugOverlayTree.messages!!.add("tag: message5")

        debugOverlayTree.logForTest(Log.DEBUG, "tag", "message6", null)

        verify(textView, times(1)).setText(
            "tag: message2\n" +
                "tag: message3\n" +
                "tag: message4\n" +
                "tag: message5\n" +
                "tag: message6",
        )
    }

    @Test
    fun simpleActivityLifecycleCallbacks_onActivityStarted() {
        debugOverlayTree.registeredActivities = registeredActivitiesMock
        debugOverlayTree.runningActivities = runningActivitiesMock
        debugOverlayTree.registeredAndRunningActivities = registeredAndRunningActivitiesMock
        debugOverlayTree.overlayView = overlayView
        run {
            whenever(registeredActivitiesMock.contains(activity)).thenReturn(false)

            debugOverlayTree.activityLifecycleCallbacks.onActivityStarted(activity)

            verify(registeredAndRunningActivitiesMock, never()).add(activity)
            verify(overlayView, never()).show()
        }
        reset(overlayView, registeredActivitiesMock, runningActivitiesMock, registeredAndRunningActivitiesMock)
        run {
            whenever(registeredActivitiesMock.contains(activity)).thenReturn(true)

            debugOverlayTree.activityLifecycleCallbacks.onActivityStarted(activity)

            verify(runningActivitiesMock, times(1)).add(activity)
            verify(overlayView, times(1)).show()
        }
    }

    @Test
    fun simpleActivityLifecycleCallbacks_onActivityStopped() {
        debugOverlayTree.overlayView = overlayView
        debugOverlayTree.registeredActivities = registeredActivitiesMock
        debugOverlayTree.runningActivities = runningActivitiesMock
        debugOverlayTree.registeredAndRunningActivities = registeredAndRunningActivitiesMock
        run {
            whenever(registeredActivitiesMock.contains(activity)).thenReturn(false)
            whenever(registeredActivitiesMock.isEmpty()).thenReturn(false)

            debugOverlayTree.activityLifecycleCallbacks.onActivityStopped(activity)

            verify(registeredAndRunningActivitiesMock, never()).remove(activity)
            verify(overlayView, never()).hide()
            verify(runningActivitiesMock, times(1)).remove(activity)
        }
        run {
            reset(overlayView, registeredActivitiesMock, runningActivitiesMock, registeredAndRunningActivitiesMock)
            run {
                whenever(registeredActivitiesMock.contains(activity)).thenReturn(true)
                whenever(registeredActivitiesMock.isEmpty()).thenReturn(false)
                whenever(registeredAndRunningActivitiesMock.isEmpty()).thenReturn(false)

                debugOverlayTree.activityLifecycleCallbacks.onActivityStopped(activity)

                verify(registeredAndRunningActivitiesMock, times(1)).isEmpty()
                verify(overlayView, never()).hide()
                verify(runningActivitiesMock, times(1)).remove(activity)
            }
            reset(overlayView, registeredActivitiesMock, runningActivitiesMock, registeredAndRunningActivitiesMock)
            run {
                whenever(registeredActivitiesMock.contains(activity)).thenReturn(true)
                whenever(registeredActivitiesMock.isEmpty()).thenReturn(false)
                whenever(registeredAndRunningActivitiesMock.isEmpty()).thenReturn(true)

                debugOverlayTree.activityLifecycleCallbacks.onActivityStopped(activity)

                verify(registeredAndRunningActivitiesMock, times(1)).isEmpty()
                verify(overlayView, times(1)).hide()
                verify(runningActivitiesMock, times(1)).remove(activity)
            }
            reset(overlayView, registeredActivitiesMock, runningActivitiesMock, registeredAndRunningActivitiesMock)
            run {
                whenever(registeredActivitiesMock.contains(activity)).thenReturn(false)
                whenever(registeredActivitiesMock.isEmpty()).thenReturn(true)
                whenever(registeredAndRunningActivitiesMock.isEmpty()).thenReturn(false)

                debugOverlayTree.activityLifecycleCallbacks.onActivityStopped(activity)

                verify(registeredAndRunningActivitiesMock, times(1)).isEmpty()
                verify(overlayView, never()).hide()
                verify(runningActivitiesMock, times(1)).remove(activity)
            }
            reset(overlayView, registeredActivitiesMock, runningActivitiesMock, registeredAndRunningActivitiesMock)
            run {
                whenever(registeredActivitiesMock.contains(activity)).thenReturn(false)
                whenever(registeredActivitiesMock.isEmpty()).thenReturn(true)
                whenever(registeredAndRunningActivitiesMock.isEmpty()).thenReturn(true)

                debugOverlayTree.activityLifecycleCallbacks.onActivityStopped(activity)

                verify(registeredAndRunningActivitiesMock, times(1)).isEmpty()
                verify(overlayView, times(1)).hide()
                verify(runningActivitiesMock, times(1)).remove(activity)
            }
            reset(overlayView, registeredActivitiesMock, runningActivitiesMock, registeredAndRunningActivitiesMock)
        }
    }

    @Test
    fun log_rendersOnlyThroughMainThreadPost_neverDirectly() {
        debugOverlayTree.initialize(application)
        debugOverlayTree.overlayView = overlayView
        val spied = spy(debugOverlayTree)
        val posted: MutableList<Runnable> = Collections.synchronizedList(ArrayList())
        doAnswer { invocation ->
            posted.add(invocation.getArgument<Runnable>(0))
            null
        }.`when`(spied).postToMainThread(any(Runnable::class.java))

        spied.logForTest(Log.DEBUG, "tag", "message", null)

        verify(textView, never()).setText(anyString())
        assertThat(posted.size, isEqualTo(1))

        posted.get(0).run()

        verify(textView, times(1)).setText("tag: message")
    }

    @Test
    fun log_highFrequencyLogging_coalescesToSinglePendingRender() {
        debugOverlayTree.initialize(application)
        debugOverlayTree.overlayView = overlayView
        val spied = spy(debugOverlayTree)
        val posted: MutableList<Runnable> = Collections.synchronizedList(ArrayList())
        doAnswer { invocation ->
            posted.add(invocation.getArgument<Runnable>(0))
            null
        }.`when`(spied).postToMainThread(any(Runnable::class.java))

        for (i in 0 until 500) {
            spied.logForTest(Log.DEBUG, "tag", "message$i", null)
        }

        assertThat("logging must coalesce onto a single pending render", posted.size, isEqualTo(1))
        verify(textView, never()).setText(anyString())

        posted.get(0).run()

        verify(textView, times(1)).setText(
            "tag: message495\ntag: message496\ntag: message497\ntag: message498\ntag: message499",
        )
    }

    @Test
    fun log_concurrentThreads_doesNotCorruptBufferOrThrow() {
        debugOverlayTree.initialize(application)
        debugOverlayTree.overlayView = overlayView

        val threadCount = 8
        val perThread = 200
        val startLatch = CountDownLatch(1)
        val doneLatch = CountDownLatch(threadCount)
        val errors: MutableList<Throwable> = Collections.synchronizedList(ArrayList())

        for (t in 0 until threadCount) {
            val thread = Thread {
                try {
                    startLatch.await()
                    for (i in 0 until perThread) {
                        debugOverlayTree.logForTest(Log.DEBUG, "t$t", "m$i", null)
                    }
                } catch (e: Throwable) {
                    errors.add(e)
                } finally {
                    doneLatch.countDown()
                }
            }
            thread.start()
        }
        startLatch.countDown()

        assertThat(doneLatch.await(10, TimeUnit.SECONDS), isEqualTo(true))
        assertThat(errors.isEmpty(), isEqualTo(true))
        assertThat(debugOverlayTree.messages!!.size, isEqualTo(debugOverlayTree.maxLines))

        val linePattern = Pattern.compile("^t\\d+: m\\d+$")
        for (line in debugOverlayTree.messages!!) {
            assertThat(linePattern.matcher(line).matches(), isEqualTo(true))
        }
    }

    @Test
    fun setThreshold_onOtherThread_isVisibleWhenLoggingFromAnotherThread() {
        debugOverlayTree.initialize(application)
        debugOverlayTree.overlayView = overlayView

        val errors: MutableList<Throwable> = Collections.synchronizedList(ArrayList())
        val thresholdSetLatch = CountDownLatch(1)

        val writerThread = Thread {
            try {
                debugOverlayTree.setThreshold(Log.ERROR)
            } catch (e: Throwable) {
                errors.add(e)
            } finally {
                thresholdSetLatch.countDown()
            }
        }
        writerThread.start()
        assertThat(thresholdSetLatch.await(10, TimeUnit.SECONDS), isEqualTo(true))
        writerThread.join(10_000)

        val loggingDoneLatch = CountDownLatch(1)
        val loggerThread = Thread {
            try {
                debugOverlayTree.logForTest(Log.DEBUG, "tag", "filtered out", null)
                debugOverlayTree.logForTest(Log.ERROR, "tag", "passes threshold", null)
            } catch (e: Throwable) {
                errors.add(e)
            } finally {
                loggingDoneLatch.countDown()
            }
        }
        loggerThread.start()
        assertThat(loggingDoneLatch.await(10, TimeUnit.SECONDS), isEqualTo(true))
        loggerThread.join(10_000)

        assertThat(errors.isEmpty(), isEqualTo(true))
        verify(textView, times(1)).setText("tag: passes threshold")
    }

    @Test
    fun simpleActivityLifecycleCallbacks_onActivityDestroyed() {
        run {
            debugOverlayTree.registeredActivities = registeredActivitiesMock

            debugOverlayTree.activityLifecycleCallbacks.onActivityDestroyed(activity)

            verify(registeredActivitiesMock, times(1)).remove(activity)
        }
    }
}
