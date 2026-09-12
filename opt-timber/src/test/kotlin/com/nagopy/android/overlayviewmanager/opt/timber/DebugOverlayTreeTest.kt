package com.nagopy.android.overlayviewmanager.opt.timber

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Looper
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
import org.hamcrest.CoreMatchers.sameInstance
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenever
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.lang.reflect.Field
import java.util.ArrayDeque
import java.util.ArrayList
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Reflection helpers for `DebugOverlayTree`'s private construction/test
 * seams. Kotlin's visibility model has no equivalent to Java's
 * package-private, so unlike the previous same-package Java test, this
 * Kotlin test cannot reach `private` members directly; these helpers use
 * `java.lang.reflect` with `setAccessible(true)` instead, the same
 * technique any other caller trying (and failing) to reach these members
 * without reflection would need.
 */
private fun newDebugOverlayTree(): DebugOverlayTree {
    val constructor = DebugOverlayTree::class.java.getDeclaredConstructor()
    constructor.isAccessible = true
    return constructor.newInstance()
}

private fun reflectField(name: String): Field {
    val field = DebugOverlayTree::class.java.getDeclaredField(name)
    field.isAccessible = true
    return field
}

// A companion object's backing field -- for both a `const val` and a plain
// `private var` such as INSTANCE -- is generated as a static field on the
// outer class itself, not as an instance field on the Companion class, so
// these are looked up on DebugOverlayTree::class.java with a null (static)
// target, not on DebugOverlayTree.Companion.
private fun reflectStaticField(name: String): Field {
    val field = DebugOverlayTree::class.java.getDeclaredField(name)
    field.isAccessible = true
    return field
}

@Suppress("UNCHECKED_CAST")
private var DebugOverlayTree.reflectedOverlayView: OverlayView<TextView>?
    get() = reflectField("overlayView").get(this) as OverlayView<TextView>?
    set(value) = reflectField("overlayView").set(this, value)

@Suppress("UNCHECKED_CAST")
private var DebugOverlayTree.reflectedMessages: ArrayDeque<String>?
    get() = reflectField("messages").get(this) as ArrayDeque<String>?
    set(value) = reflectField("messages").set(this, value)

@Suppress("UNCHECKED_CAST")
private var DebugOverlayTree.reflectedRegisteredActivities: WeakReferenceCache<Activity>?
    get() = reflectField("registeredActivities").get(this) as WeakReferenceCache<Activity>?
    set(value) = reflectField("registeredActivities").set(this, value)

@Suppress("UNCHECKED_CAST")
private var DebugOverlayTree.reflectedRunningActivities: WeakReferenceCache<Activity>?
    get() = reflectField("runningActivities").get(this) as WeakReferenceCache<Activity>?
    set(value) = reflectField("runningActivities").set(this, value)

@Suppress("UNCHECKED_CAST")
private var DebugOverlayTree.reflectedRegisteredAndRunningActivities: WeakReferenceCache<Activity>?
    get() = reflectField("registeredAndRunningActivities").get(this) as WeakReferenceCache<Activity>?
    set(value) = reflectField("registeredAndRunningActivities").set(this, value)

private var DebugOverlayTree.reflectedThreshold: Int
    get() = reflectField("threshold").getInt(this)
    set(value) = reflectField("threshold").setInt(this, value)

private val DebugOverlayTree.reflectedMaxLines: Int
    get() = reflectField("maxLines").getInt(this)

@Suppress("UNCHECKED_CAST")
private val DebugOverlayTree.reflectedActivityLifecycleCallbacks: Application.ActivityLifecycleCallbacks
    get() = reflectField("activityLifecycleCallbacks").get(this) as Application.ActivityLifecycleCallbacks

@Suppress("UNCHECKED_CAST")
private var reflectedInstance: DebugOverlayTree
    get() = reflectStaticField("INSTANCE").get(null) as DebugOverlayTree
    set(value) = reflectStaticField("INSTANCE").set(null, value)

private val reflectedDefaultMaxLines: Int
    get() = reflectStaticField("DEFAULT_MAX_LINES").getInt(null)

private fun DebugOverlayTree.callInitialize(application: Application) {
    val method = DebugOverlayTree::class.java.getDeclaredMethod("initialize", Application::class.java)
    method.isAccessible = true
    method.invoke(this, application)
}

private fun DebugOverlayTree.callLog(priority: Int, tag: String?, message: String, t: Throwable?) {
    val method = DebugOverlayTree::class.java.getDeclaredMethod(
        "log",
        Int::class.javaPrimitiveType,
        String::class.java,
        String::class.java,
        Throwable::class.java,
    )
    method.isAccessible = true
    method.invoke(this, priority, tag, message, t)
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.M], manifest = Config.NONE)
class DebugOverlayTreeTest {

    private lateinit var debugOverlayTree: DebugOverlayTree
    private lateinit var application: Application

    @Mock
    private lateinit var overlayWindowManager: OverlayWindowManager

    @Mock
    private lateinit var windowManager: WindowManager

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

    /**
     * Resets core's `OverlayViewManager` singleton by reflection, purely a
     * test concern (no reset API is added to production code). Today's
     * Java core declares a package-private, eagerly-created static field
     * named `INSTANCE` that `init(app)` re-configures in place -- nulling
     * it would NPE inside `init()`, so it is left alone and `init(app)`
     * just re-initializes it. T04b's Kotlin core is expected to instead
     * hold a nullable singleton in a field named `instance`, which this
     * nulls before `init(app)` so each test starts from a clean singleton
     * with its own fresh Robolectric `Application`. If neither field
     * exists, the class shape changed in a way this helper does not
     * understand, so it fails loudly instead of silently no-op'ing.
     */
    private fun resetOverlayViewManagerSingletonForTest() {
        val clazz = OverlayViewManager::class.java
        val lowerField = try {
            clazz.getDeclaredField("instance")
        } catch (expected: NoSuchFieldException) {
            null
        }
        if (lowerField != null) {
            lowerField.isAccessible = true
            lowerField.set(null, null)
            return
        }

        val upperField = try {
            clazz.getDeclaredField("INSTANCE")
        } catch (expected: NoSuchFieldException) {
            null
        }
        if (upperField != null) {
            // Current Java core: re-initialization in place is allowed, no reset needed.
            return
        }

        throw AssertionError("Expected a static 'instance' or 'INSTANCE' field on ${clazz.name}")
    }

    /**
     * Concentrates core setup in one place so this suite keeps passing once
     * T04b's Kotlin `OverlayViewManager` starts rejecting a null main
     * looper and re-initialization with a different `Application`:
     * Robolectric supplies a real main looper, and resetting the singleton
     * above lets each test start from a clean state with its own fresh
     * Robolectric `Application`.
     */
    private fun initCoreForTest(): Application {
        resetOverlayViewManagerSingletonForTest()

        val app = spy(RuntimeEnvironment.getApplication())
        whenever(app.getApplicationContext()).thenReturn(app)
        OverlayViewManager.init(app)
        OverlayWindowManager.setApplicationInstance(overlayWindowManager)
        OverlayWindowManager.initApplicationInstance(windowManager)
        return app
    }

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        application = initCoreForTest()

        debugOverlayTree = newDebugOverlayTree()
        assertThat(debugOverlayTree.reflectedOverlayView, isEqualTo(nullValue()))

        whenever(overlayView.view).thenReturn(textView)
    }

    @After
    fun tearDown() {
    }

    @Test(expected = IllegalStateException::class)
    fun getInstance_noView() {
        reflectedInstance.reflectedOverlayView = null
        DebugOverlayTree.getInstance()
    }

    @Test
    fun getInstance() {
        reflectedInstance.reflectedOverlayView = overlayView
        DebugOverlayTree.getInstance()
    }

    @Test
    fun init() {
        // initialize() is private, so Mockito cannot generate a subclass
        // override to verify a call on a mock; instead, swap in a known
        // fresh (uninitialized) instance and confirm initialize() ran on
        // that same instance by checking its resulting state.
        val freshTree = newDebugOverlayTree()
        reflectedInstance = freshTree
        assertThat(freshTree.reflectedOverlayView, isEqualTo(nullValue()))

        val result = DebugOverlayTree.init(application)

        assertThat(result, isEqualTo(sameInstance(freshTree)))
        assertThat(freshTree.reflectedOverlayView, isEqualTo(notNullValue()))
        assertThat(freshTree.reflectedMessages, isEqualTo(notNullValue()))
    }

    @Test
    fun initialize() {
        debugOverlayTree.callInitialize(application)

        assertThat(debugOverlayTree.reflectedMessages, isEqualTo(notNullValue()))
        assertThat(debugOverlayTree.reflectedThreshold, isEqualTo(Log.DEBUG))
        assertThat(debugOverlayTree.reflectedMaxLines, isEqualTo(5))
        assertThat(debugOverlayTree.reflectedOverlayView, isEqualTo(notNullValue()))
        assertThat(debugOverlayTree.reflectedRegisteredActivities, isEqualTo(notNullValue()))
        verify(application, times(1)).registerActivityLifecycleCallbacks(debugOverlayTree.reflectedActivityLifecycleCallbacks)
    }

    @Test
    fun setThreshold() {
        debugOverlayTree.reflectedThreshold = 0

        debugOverlayTree.setThreshold(1)
        assertThat(debugOverlayTree.reflectedThreshold, isEqualTo(1))
    }

    @Test
    fun setMaxLines() {
        debugOverlayTree.callInitialize(application)
        debugOverlayTree.reflectedOverlayView = overlayView

        debugOverlayTree.setMaxLines(1)

        assertThat(debugOverlayTree.reflectedMaxLines, isEqualTo(1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun setMaxLines_zero_throws() {
        debugOverlayTree.callInitialize(application)
        debugOverlayTree.reflectedOverlayView = overlayView

        debugOverlayTree.setMaxLines(0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun setMaxLines_negative_throws() {
        debugOverlayTree.callInitialize(application)
        debugOverlayTree.reflectedOverlayView = overlayView

        debugOverlayTree.setMaxLines(-1)
    }

    @Test
    fun setMaxLines_invalid_doesNotChangeState() {
        debugOverlayTree.callInitialize(application)
        debugOverlayTree.reflectedOverlayView = overlayView

        try {
            debugOverlayTree.setMaxLines(0)
            fail("expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            // no-op
        }

        assertThat(debugOverlayTree.reflectedMaxLines, isEqualTo(reflectedDefaultMaxLines))
    }

    @Test
    fun setMaxLines_shrink_trimsBufferAndRerenders() {
        debugOverlayTree.callInitialize(application)
        debugOverlayTree.reflectedOverlayView = overlayView
        for (i in 1..5) {
            debugOverlayTree.callLog(Log.DEBUG, "tag", "message$i", null)
        }
        reset(textView)

        debugOverlayTree.setMaxLines(2)

        assertThat(debugOverlayTree.reflectedMessages!!.size, isEqualTo(2))
        verify(textView, times(1)).setText("tag: message4\ntag: message5")
    }

    @Test
    fun setMaxLines_equal_doesNotTrimOrRerender() {
        debugOverlayTree.callInitialize(application)
        debugOverlayTree.reflectedOverlayView = overlayView
        for (i in 1..5) {
            debugOverlayTree.callLog(Log.DEBUG, "tag", "message$i", null)
        }
        reset(textView)

        debugOverlayTree.setMaxLines(5)

        assertThat(debugOverlayTree.reflectedMessages!!.size, isEqualTo(5))
        verify(textView, never()).setText(anyString())
    }

    @Test
    fun setMaxLines_grow_doesNotTrimExistingBuffer() {
        debugOverlayTree.callInitialize(application)
        debugOverlayTree.reflectedOverlayView = overlayView
        for (i in 1..5) {
            debugOverlayTree.callLog(Log.DEBUG, "tag", "message$i", null)
        }
        reset(textView)

        debugOverlayTree.setMaxLines(10)

        assertThat(debugOverlayTree.reflectedMaxLines, isEqualTo(10))
        assertThat(debugOverlayTree.reflectedMessages!!.size, isEqualTo(5))
        verify(textView, never()).setText(anyString())
    }

    @Test
    fun register_notRunning() {
        debugOverlayTree.reflectedRegisteredActivities = registeredActivitiesMock
        debugOverlayTree.reflectedRunningActivities = runningActivitiesMock
        debugOverlayTree.reflectedRegisteredAndRunningActivities = registeredAndRunningActivitiesMock
        whenever(runningActivitiesMock.contains(activity)).thenReturn(false)

        debugOverlayTree.register(activity)

        verify(registeredActivitiesMock, times(1)).add(activity)
        verify(runningActivitiesMock, times(1)).contains(activity)
        verify(registeredAndRunningActivitiesMock, never()).add(activity)
        verify(overlayView, never()).show()
    }

    @Test
    fun register_running() {
        debugOverlayTree.reflectedOverlayView = overlayView
        debugOverlayTree.reflectedRegisteredActivities = registeredActivitiesMock
        debugOverlayTree.reflectedRunningActivities = runningActivitiesMock
        debugOverlayTree.reflectedRegisteredAndRunningActivities = registeredAndRunningActivitiesMock
        whenever(runningActivitiesMock.contains(activity)).thenReturn(true)

        debugOverlayTree.register(activity)

        verify(registeredActivitiesMock, times(1)).add(activity)
        verify(runningActivitiesMock, times(1)).contains(activity)
        verify(registeredAndRunningActivitiesMock, times(1)).add(activity)
        verify(overlayView, times(1)).show()
    }

    @Test
    fun log_threshold() {
        debugOverlayTree.callInitialize(application)
        debugOverlayTree.reflectedOverlayView = overlayView

        debugOverlayTree.callLog(Log.VERBOSE, "tag", "message", null)

        verify(textView, never()).setText(anyString())
    }

    @Test
    fun log() {
        debugOverlayTree.callInitialize(application)
        debugOverlayTree.reflectedOverlayView = overlayView

        debugOverlayTree.callLog(Log.DEBUG, "tag", "message", null)

        verify(textView, times(1)).setText("tag: message")
    }

    @Test
    fun log_multiLine() {
        debugOverlayTree.callInitialize(application)
        debugOverlayTree.reflectedOverlayView = overlayView
        debugOverlayTree.reflectedMessages!!.add("tag: message")

        debugOverlayTree.callLog(Log.DEBUG, "tag", "message2", null)

        verify(textView, times(1)).setText("tag: message\ntag: message2")
    }

    @Test
    fun log_multiLine_max() {
        debugOverlayTree.callInitialize(application)
        debugOverlayTree.reflectedOverlayView = overlayView
        debugOverlayTree.reflectedMessages!!.add("tag: message1")
        debugOverlayTree.reflectedMessages!!.add("tag: message2")
        debugOverlayTree.reflectedMessages!!.add("tag: message3")
        debugOverlayTree.reflectedMessages!!.add("tag: message4")
        debugOverlayTree.reflectedMessages!!.add("tag: message5")

        debugOverlayTree.callLog(Log.DEBUG, "tag", "message6", null)

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
        debugOverlayTree.reflectedRegisteredActivities = registeredActivitiesMock
        debugOverlayTree.reflectedRunningActivities = runningActivitiesMock
        debugOverlayTree.reflectedRegisteredAndRunningActivities = registeredAndRunningActivitiesMock
        debugOverlayTree.reflectedOverlayView = overlayView
        run {
            whenever(registeredActivitiesMock.contains(activity)).thenReturn(false)

            debugOverlayTree.reflectedActivityLifecycleCallbacks.onActivityStarted(activity)

            verify(registeredAndRunningActivitiesMock, never()).add(activity)
            verify(overlayView, never()).show()
        }
        reset(overlayView, registeredActivitiesMock, runningActivitiesMock, registeredAndRunningActivitiesMock)
        run {
            whenever(registeredActivitiesMock.contains(activity)).thenReturn(true)

            debugOverlayTree.reflectedActivityLifecycleCallbacks.onActivityStarted(activity)

            verify(runningActivitiesMock, times(1)).add(activity)
            verify(overlayView, times(1)).show()
        }
    }

    @Test
    fun simpleActivityLifecycleCallbacks_onActivityStopped() {
        debugOverlayTree.reflectedOverlayView = overlayView
        debugOverlayTree.reflectedRegisteredActivities = registeredActivitiesMock
        debugOverlayTree.reflectedRunningActivities = runningActivitiesMock
        debugOverlayTree.reflectedRegisteredAndRunningActivities = registeredAndRunningActivitiesMock
        run {
            whenever(registeredActivitiesMock.contains(activity)).thenReturn(false)
            whenever(registeredActivitiesMock.isEmpty()).thenReturn(false)

            debugOverlayTree.reflectedActivityLifecycleCallbacks.onActivityStopped(activity)

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

                debugOverlayTree.reflectedActivityLifecycleCallbacks.onActivityStopped(activity)

                verify(registeredAndRunningActivitiesMock, times(1)).isEmpty()
                verify(overlayView, never()).hide()
                verify(runningActivitiesMock, times(1)).remove(activity)
            }
            reset(overlayView, registeredActivitiesMock, runningActivitiesMock, registeredAndRunningActivitiesMock)
            run {
                whenever(registeredActivitiesMock.contains(activity)).thenReturn(true)
                whenever(registeredActivitiesMock.isEmpty()).thenReturn(false)
                whenever(registeredAndRunningActivitiesMock.isEmpty()).thenReturn(true)

                debugOverlayTree.reflectedActivityLifecycleCallbacks.onActivityStopped(activity)

                verify(registeredAndRunningActivitiesMock, times(1)).isEmpty()
                verify(overlayView, times(1)).hide()
                verify(runningActivitiesMock, times(1)).remove(activity)
            }
            reset(overlayView, registeredActivitiesMock, runningActivitiesMock, registeredAndRunningActivitiesMock)
            run {
                whenever(registeredActivitiesMock.contains(activity)).thenReturn(false)
                whenever(registeredActivitiesMock.isEmpty()).thenReturn(true)
                whenever(registeredAndRunningActivitiesMock.isEmpty()).thenReturn(false)

                debugOverlayTree.reflectedActivityLifecycleCallbacks.onActivityStopped(activity)

                verify(registeredAndRunningActivitiesMock, times(1)).isEmpty()
                verify(overlayView, never()).hide()
                verify(runningActivitiesMock, times(1)).remove(activity)
            }
            reset(overlayView, registeredActivitiesMock, runningActivitiesMock, registeredAndRunningActivitiesMock)
            run {
                whenever(registeredActivitiesMock.contains(activity)).thenReturn(false)
                whenever(registeredActivitiesMock.isEmpty()).thenReturn(true)
                whenever(registeredAndRunningActivitiesMock.isEmpty()).thenReturn(true)

                debugOverlayTree.reflectedActivityLifecycleCallbacks.onActivityStopped(activity)

                verify(registeredAndRunningActivitiesMock, times(1)).isEmpty()
                verify(overlayView, times(1)).hide()
                verify(runningActivitiesMock, times(1)).remove(activity)
            }
            reset(overlayView, registeredActivitiesMock, runningActivitiesMock, registeredAndRunningActivitiesMock)
        }
    }

    @Test
    fun log_rendersOnlyThroughMainThreadPost_neverDirectly() {
        debugOverlayTree.callInitialize(application)
        debugOverlayTree.reflectedOverlayView = overlayView

        // Robolectric provides a real main looper; calling log() from a
        // background thread takes postToMainThread's posting branch
        // instead of running synchronously, so the render is genuinely
        // pending until the shadow looper is drained below.
        val loggingThread = Thread {
            debugOverlayTree.callLog(Log.DEBUG, "tag", "message", null)
        }
        loggingThread.start()
        loggingThread.join(10_000)

        verify(textView, never()).setText(anyString())

        shadowOf(Looper.getMainLooper()).idle()

        verify(textView, times(1)).setText("tag: message")
    }

    @Test
    fun log_highFrequencyLogging_coalescesToSinglePendingRender() {
        debugOverlayTree.callInitialize(application)
        debugOverlayTree.reflectedOverlayView = overlayView

        // All 500 calls run on one background thread before the shadow
        // looper is drained, so only the first can win the renderPending
        // gate in scheduleRender(); the rest coalesce onto it. If
        // coalescing failed, draining below would call setText more than
        // once and fail the times(1) verification.
        val loggingThread = Thread {
            for (i in 0 until 500) {
                debugOverlayTree.callLog(Log.DEBUG, "tag", "message$i", null)
            }
        }
        loggingThread.start()
        loggingThread.join(10_000)

        verify(textView, never()).setText(anyString())

        shadowOf(Looper.getMainLooper()).idle()

        verify(textView, times(1)).setText(
            "tag: message495\ntag: message496\ntag: message497\ntag: message498\ntag: message499",
        )
    }

    @Test
    fun log_concurrentThreads_doesNotCorruptBufferOrThrow() {
        debugOverlayTree.callInitialize(application)
        debugOverlayTree.reflectedOverlayView = overlayView

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
                        debugOverlayTree.callLog(Log.DEBUG, "t$t", "m$i", null)
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
        assertThat(debugOverlayTree.reflectedMessages!!.size, isEqualTo(debugOverlayTree.reflectedMaxLines))

        val linePattern = Pattern.compile("^t\\d+: m\\d+$")
        for (line in debugOverlayTree.reflectedMessages!!) {
            assertThat(linePattern.matcher(line).matches(), isEqualTo(true))
        }
    }

    @Test
    fun setThreshold_onOtherThread_isVisibleWhenLoggingFromAnotherThread() {
        debugOverlayTree.callInitialize(application)
        debugOverlayTree.reflectedOverlayView = overlayView

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
                debugOverlayTree.callLog(Log.DEBUG, "tag", "filtered out", null)
                debugOverlayTree.callLog(Log.ERROR, "tag", "passes threshold", null)
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

        shadowOf(Looper.getMainLooper()).idle()

        verify(textView, times(1)).setText("tag: passes threshold")
    }

    @Test
    fun simpleActivityLifecycleCallbacks_onActivityDestroyed() {
        run {
            debugOverlayTree.reflectedRegisteredActivities = registeredActivitiesMock

            debugOverlayTree.reflectedActivityLifecycleCallbacks.onActivityDestroyed(activity)

            verify(registeredActivitiesMock, times(1)).remove(activity)
        }
    }
}
