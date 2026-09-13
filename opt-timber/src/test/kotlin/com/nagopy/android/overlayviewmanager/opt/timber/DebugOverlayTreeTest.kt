package com.nagopy.android.overlayviewmanager.opt.timber

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import com.nagopy.android.overlayviewmanager.OverlayFailure
import com.nagopy.android.overlayviewmanager.OverlayState
import com.nagopy.android.overlayviewmanager.OverlayView
import com.nagopy.android.overlayviewmanager.OverlayViewManager
import com.nagopy.android.overlayviewmanager.internal.OverlayWindowManager
import com.nagopy.android.overlayviewmanager.internal.WeakReferenceCache
import org.hamcrest.CoreMatchers.`is` as isEqualTo
import org.hamcrest.CoreMatchers.instanceOf
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.CoreMatchers.nullValue
import org.hamcrest.CoreMatchers.sameInstance
import org.junit.After
import org.junit.Assert.assertThat
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.reset
import org.mockito.Mockito.spy
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenever
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowSettings
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
        ShadowSettings.setCanDrawOverlays(true)

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
        // looper is drained, so only the first claims the pending render
        // slot for the current generation; the rest coalesce onto it. If
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
            debugOverlayTree.reflectedRunningActivities = runningActivitiesMock
            debugOverlayTree.reflectedRegisteredAndRunningActivities = registeredAndRunningActivitiesMock

            debugOverlayTree.reflectedActivityLifecycleCallbacks.onActivityDestroyed(activity)

            // L4: onActivityDestroyed must remove the Activity from every cache, not only
            // registeredActivities, so no destroyed Activity is retained anywhere.
            verify(registeredActivitiesMock, times(1)).remove(activity)
            verify(runningActivitiesMock, times(1)).remove(activity)
            verify(registeredAndRunningActivitiesMock, times(1)).remove(activity)
        }
    }

    // --- L3: init idempotence -------------------------------------------------------------

    @Test
    fun init_sameApplication_isNoOpAndDoesNotReRegisterCallbacks() {
        val freshTree = newDebugOverlayTree()
        reflectedInstance = freshTree

        val first = DebugOverlayTree.init(application)
        val handleAfterFirstInit = freshTree.reflectedOverlayView
        val callback = freshTree.reflectedActivityLifecycleCallbacks

        val second = DebugOverlayTree.init(application)

        assertThat(second, isEqualTo(sameInstance(first)))
        assertThat(freshTree.reflectedOverlayView, isEqualTo(sameInstance(handleAfterFirstInit)))
        verify(application, times(1)).registerActivityLifecycleCallbacks(callback)
    }

    @Test
    fun init_differentApplication_throwsAndDoesNotChangeState() {
        val freshTree = newDebugOverlayTree()
        reflectedInstance = freshTree
        DebugOverlayTree.init(application)
        val handleBefore = freshTree.reflectedOverlayView
        val messagesBefore = freshTree.reflectedMessages
        val otherApplication = mock(Application::class.java)

        try {
            DebugOverlayTree.init(otherApplication)
            fail("expected IllegalStateException")
        } catch (expected: IllegalStateException) {
            // no-op
        }

        assertThat(freshTree.reflectedOverlayView, isEqualTo(sameInstance(handleBefore)))
        assertThat(freshTree.reflectedMessages, isEqualTo(sameInstance(messagesBefore)))
        verify(otherApplication, never()).registerActivityLifecycleCallbacks(any())
    }

    @Test
    fun init_afterSuccessfulDispose_reinitializesCleanlyWithExactlyOneCallbackAndOneHandle() {
        val freshTree = newDebugOverlayTree()
        reflectedInstance = freshTree
        DebugOverlayTree.init(application)
        val callback = freshTree.reflectedActivityLifecycleCallbacks
        val firstHandle = freshTree.reflectedOverlayView

        val disposeResult = freshTree.dispose()
        assertThat(disposeResult.isSuccess, isEqualTo(true))
        assertThat(freshTree.reflectedOverlayView, isEqualTo(nullValue()))

        val reInitialized = DebugOverlayTree.init(application)

        assertThat(reInitialized, isEqualTo(sameInstance(freshTree)))
        assertThat(freshTree.reflectedOverlayView, isEqualTo(notNullValue()))
        assertThat(freshTree.reflectedOverlayView, not(sameInstance(firstHandle)))
        // Exactly one live registration: register() called once per init, unregister() called
        // once by the successful dispose in between -- net one active callback.
        verify(application, times(2)).registerActivityLifecycleCallbacks(callback)
        verify(application, times(1)).unregisterActivityLifecycleCallbacks(callback)
    }

    // --- L2: dispose() -----------------------------------------------------------------------

    @Test
    fun dispose_success_releasesEverythingAndIsIdempotent() {
        debugOverlayTree.callInitialize(application)
        val callback = debugOverlayTree.reflectedActivityLifecycleCallbacks

        val result = debugOverlayTree.dispose()

        assertThat(result.isSuccess, isEqualTo(true))
        assertThat(debugOverlayTree.reflectedOverlayView, isEqualTo(nullValue()))
        assertThat(debugOverlayTree.reflectedMessages, isEqualTo(nullValue()))
        assertThat(debugOverlayTree.reflectedRegisteredActivities, isEqualTo(nullValue()))
        assertThat(debugOverlayTree.reflectedRunningActivities, isEqualTo(nullValue()))
        assertThat(debugOverlayTree.reflectedRegisteredAndRunningActivities, isEqualTo(nullValue()))
        verify(application, times(1)).unregisterActivityLifecycleCallbacks(callback)

        reflectedInstance = debugOverlayTree
        try {
            DebugOverlayTree.getInstance()
            fail("expected IllegalStateException")
        } catch (expected: IllegalStateException) {
            // no-op
        }

        val secondDispose = debugOverlayTree.dispose()
        assertThat(secondDispose.isSuccess, isEqualTo(true))
        assertThat(secondDispose.changed, isEqualTo(false))
        assertThat(secondDispose.state, isEqualTo(OverlayState.DISPOSED))
        // Still just once: a repeated dispose after success never touches the callback again.
        verify(application, times(1)).unregisterActivityLifecycleCallbacks(callback)
    }

    @Test
    fun dispose_failedUnderlyingDisposal_isTransactionalAndRetryable() {
        val backend = RecordingBackend(hideFailure = RuntimeException("boom"))
        OverlayWindowManager.setApplicationInstance(backend)
        debugOverlayTree.callInitialize(application)
        val handle = debugOverlayTree.reflectedOverlayView!!
        assertThat(handle.show().isSuccess, isEqualTo(true))
        assertThat(handle.state, isEqualTo(OverlayState.ATTACHED))

        val result = debugOverlayTree.dispose()

        assertThat(result.isSuccess, isEqualTo(false))
        assertThat(backend.hideCalls, isEqualTo(1))
        // Nothing is cleared on a failed dispose: the handle, caches and buffer are all intact.
        assertThat(debugOverlayTree.reflectedOverlayView, isEqualTo(sameInstance(handle)))
        assertThat(debugOverlayTree.reflectedMessages, isEqualTo(notNullValue()))
        assertThat(debugOverlayTree.reflectedRegisteredActivities, isEqualTo(notNullValue()))

        // log() still works: the tree remains fully active after a failed dispose.
        debugOverlayTree.callLog(Log.DEBUG, "tag", "still logging", null)
        assertThat(debugOverlayTree.reflectedMessages!!.contains("tag: still logging"), isEqualTo(true))

        // A retried dispose succeeds once the backend recovers, without a new handle.
        backend.hideFailure = null
        val retryResult = debugOverlayTree.dispose()

        assertThat(retryResult.isSuccess, isEqualTo(true))
        assertThat(backend.hideCalls, isEqualTo(2))
        assertThat(debugOverlayTree.reflectedOverlayView, isEqualTo(nullValue()))
    }

    @Test
    fun setMaxLines_afterSuccessfulDispose_doesNotCrash() {
        debugOverlayTree.callInitialize(application)
        debugOverlayTree.dispose()

        debugOverlayTree.setMaxLines(3)

        assertThat(debugOverlayTree.reflectedMaxLines, isEqualTo(3))
    }

    // --- Codex finding #3: dispose() enforces the main-thread contract on every path ----------

    /**
     * Covers the branch where [DebugOverlayTree.dispose] is called while initialized (so it
     * reaches the underlying handle). This branch always had a main-thread check -- delegated to
     * the underlying `OverlayView.dispose()` -- so this asserts the pre-existing, still-correct
     * behavior rather than a fix.
     */
    @Test
    fun dispose_calledOffMainThread_whileInitialized_throwsAndChangesNothing() {
        debugOverlayTree.callInitialize(application)

        val error = disposeOnBackgroundThreadAndCaptureFailure()

        assertThat(error, instanceOf(IllegalStateException::class.java))
        assertThat(debugOverlayTree.reflectedOverlayView, isEqualTo(notNullValue()))
        assertThat(debugOverlayTree.reflectedMessages, isEqualTo(notNullValue()))
    }

    /**
     * Codex finding #3 regression: before the fix, the uninitialized/no-op early-return branch of
     * [DebugOverlayTree.dispose] (`overlayView == null`) skipped the underlying handle entirely --
     * and, with it, the underlying handle's own main-thread check -- so calling `dispose()` off
     * the main thread while never initialized silently returned a fake successful [OverlayResult]
     * instead of throwing. [DebugOverlayTree.dispose] now checks the main thread itself, as its
     * very first statement, before this branch is ever reached.
     */
    @Test
    fun dispose_calledOffMainThread_whileNeverInitialized_throwsIllegalStateException() {
        // debugOverlayTree is fresh from setUp() -- never initialized.
        val error = disposeOnBackgroundThreadAndCaptureFailure()

        assertThat(error, instanceOf(IllegalStateException::class.java))
    }

    /**
     * Codex finding #3 regression, on the OTHER no-op branch: after a successful dispose,
     * `overlayView` is `null` again, so this exercises the exact same previously-unchecked early
     * return as the never-initialized case above, but reached via a different path.
     */
    @Test
    fun dispose_calledOffMainThread_afterSuccessfulDispose_throwsIllegalStateException() {
        debugOverlayTree.callInitialize(application)
        assertThat(debugOverlayTree.dispose().isSuccess, isEqualTo(true))

        val error = disposeOnBackgroundThreadAndCaptureFailure()

        assertThat(error, instanceOf(IllegalStateException::class.java))
    }

    private fun disposeOnBackgroundThreadAndCaptureFailure(): Throwable? {
        val errors: MutableList<Throwable> = Collections.synchronizedList(ArrayList())
        val doneLatch = CountDownLatch(1)
        val thread = Thread {
            try {
                debugOverlayTree.dispose()
            } catch (e: Throwable) {
                errors.add(e)
            } finally {
                doneLatch.countDown()
            }
        }
        thread.start()
        assertThat(doneLatch.await(10, TimeUnit.SECONDS), isEqualTo(true))
        assertThat(errors.size, isEqualTo(1))
        return errors.firstOrNull()
    }

    // --- L1: denied permission is logged and non-fatal ------------------------------------

    @Test
    fun onActivityStarted_permissionDenied_isNonFatalAndLeavesTreeConsistent() {
        ShadowSettings.setCanDrawOverlays(false)
        debugOverlayTree.callInitialize(application)
        val activityForTest = Robolectric.buildActivity(Activity::class.java).create().get()

        // Not yet running: register() only records bookkeeping, no show() attempted.
        debugOverlayTree.register(activityForTest)
        // Starting it drives the first (and only) show() attempt, which the denied permission fails.
        debugOverlayTree.reflectedActivityLifecycleCallbacks.onActivityStarted(activityForTest)

        val handle = debugOverlayTree.reflectedOverlayView!!
        assertThat(handle.state, isEqualTo(OverlayState.CONFIGURED))
        assertThat(handle.lastFailure, isEqualTo(OverlayFailure.PERMISSION_DENIED))
        // Bookkeeping is unaffected by the failed show -- the tree stays internally consistent.
        assertThat(debugOverlayTree.reflectedRegisteredAndRunningActivities!!.contains(activityForTest), isEqualTo(true))
    }

    // --- L5: generation-guarded render -----------------------------------------------------

    @Test
    fun render_queuedBeforeSuccessfulDispose_isANoOp() {
        debugOverlayTree.callInitialize(application)
        val loggingThread = Thread {
            debugOverlayTree.callLog(Log.DEBUG, "tag", "queued before dispose", null)
        }
        loggingThread.start()
        loggingThread.join(10_000)

        val disposeResult = debugOverlayTree.dispose()
        assertThat(disposeResult.isSuccess, isEqualTo(true))

        // Must not throw when the already-queued render finally runs against a disposed tree.
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(debugOverlayTree.reflectedMessages, isEqualTo(nullValue()))
    }

    @Test
    fun render_queuedBeforeSuccessfulDisposeAndReinit_rendersNothingFromTheOldGeneration() {
        debugOverlayTree.callInitialize(application)
        val loggingThread = Thread {
            debugOverlayTree.callLog(Log.DEBUG, "tag", "stale message", null)
        }
        loggingThread.start()
        loggingThread.join(10_000)

        val disposeResult = debugOverlayTree.dispose()
        assertThat(disposeResult.isSuccess, isEqualTo(true))

        reflectedInstance = debugOverlayTree
        DebugOverlayTree.init(application)
        val newHandle = debugOverlayTree.reflectedOverlayView!!

        // Drains the stale render Runnable, if the generation guard failed to skip it.
        shadowOf(Looper.getMainLooper()).idle()

        assertThat(newHandle.view.text.toString(), isEqualTo(""))
    }

    /**
     * Codex finding #1 regression: a stale render queued before a successful dispose must never
     * block or lose a fresh log() issued in the NEW generation before the stale callback drains.
     * Before the fix, scheduling tracked a single renderPending boolean shared across
     * generations: the fresh log() below would lose its CAS to that stale flag (still `true` from
     * the pre-dispose schedule), and the stale callback's own generation check would then bail
     * without rendering anything, so the fresh message was rendered only much later, and only if
     * some unrelated log() call happened to arrive afterward. Tracking the pending generation
     * itself (instead of a plain flag) fixes this: a fresh log() in a new generation always
     * re-arms scheduling.
     *
     * The queue order is explicit and deterministic: every log() call runs on its own background
     * thread and is `join()`-ed before the next step, so exactly two render callbacks are queued
     * on the main thread in FIFO order -- the stale one (posted before dispose) first, the fresh
     * one (posted after re-init) second. The callbacks are then drained ONE AT A TIME with
     * `runOneTask()`: the stale callback must run first and render nothing (it belongs to a
     * generation this tree has left), and only the fresh callback may render the fresh message.
     */
    @Test
    fun log_newMessageBeforeStaleRenderDrains_staleCallbackIsNoOpAndFreshCallbackRendersOnce() {
        debugOverlayTree.callInitialize(application)
        val mainLooper = shadowOf(Looper.getMainLooper())

        // Step 1: a background log() in the OLD generation schedules (queues) a render.
        val staleLoggingThread = Thread {
            debugOverlayTree.callLog(Log.DEBUG, "tag", "stale message", null)
        }
        staleLoggingThread.start()
        staleLoggingThread.join(10_000)

        // Step 2: dispose (main thread) succeeds while that stale render callback is still
        // sitting, undrained, in the queue -- generation advances, buffer/handle released.
        val disposeResult = debugOverlayTree.dispose()
        assertThat(disposeResult.isSuccess, isEqualTo(true))

        // Step 3: re-init (main thread) -- generation advances again, a fresh handle/buffer/
        // TextView created.
        reflectedInstance = debugOverlayTree
        DebugOverlayTree.init(application)
        val newHandle = debugOverlayTree.reflectedOverlayView!!

        // Step 4: a NEW background log() in the NEW generation, issued strictly before the stale
        // callback from step 1 is drained. This is exactly the ordering Codex's finding #1
        // describes; it must schedule its own render instead of waiting on the stale callback.
        val freshLoggingThread = Thread {
            debugOverlayTree.callLog(Log.DEBUG, "tag", "fresh message", null)
        }
        freshLoggingThread.start()
        freshLoggingThread.join(10_000)

        // Step 5: nothing has been rendered yet -- both callbacks are queued, undrained.
        assertThat(newHandle.view.text.toString(), isEqualTo(""))

        // Step 6: run ONLY the first queued callback -- the stale one. It must not render the
        // fresh buffer on behalf of the new generation, nor anything from the old one.
        mainLooper.runOneTask()
        assertThat(newHandle.view.text.toString(), isEqualTo(""))

        // Step 7: run the second queued callback -- the fresh one -- which renders the fresh
        // message exactly once.
        mainLooper.runOneTask()
        assertThat(newHandle.view.text.toString(), isEqualTo("tag: fresh message"))
        assertThat(mainLooper.isIdle, isEqualTo(true))
    }

    @Test
    fun log_afterSuccessfulDispose_isSilentNoOpFromAnyThread() {
        debugOverlayTree.callInitialize(application)

        val disposeResult = debugOverlayTree.dispose()
        assertThat(disposeResult.isSuccess, isEqualTo(true))
        assertThat(debugOverlayTree.reflectedMessages, isEqualTo(nullValue()))

        val errors: MutableList<Throwable> = Collections.synchronizedList(ArrayList())
        val doneLatch = CountDownLatch(1)
        val loggingThread = Thread {
            try {
                debugOverlayTree.callLog(Log.DEBUG, "tag", "after dispose", null)
            } catch (e: Throwable) {
                errors.add(e)
            } finally {
                doneLatch.countDown()
            }
        }
        loggingThread.start()
        assertThat(doneLatch.await(10, TimeUnit.SECONDS), isEqualTo(true))
        loggingThread.join(10_000)

        assertThat(errors.isEmpty(), isEqualTo(true))
        assertThat(debugOverlayTree.reflectedMessages, isEqualTo(nullValue()))
    }

    /** Records `hide` invocations and can be told to fail/recover on demand, per test step. */
    private class RecordingBackend(var hideFailure: Throwable? = null) : OverlayWindowManager() {
        var hideCalls = 0
        override fun show(view: View, params: WindowManager.LayoutParams) = Unit
        override fun update(view: View, params: WindowManager.LayoutParams) = Unit
        override fun hide(view: View) {
            hideCalls++
            hideFailure?.let { throw it }
        }
    }
}
