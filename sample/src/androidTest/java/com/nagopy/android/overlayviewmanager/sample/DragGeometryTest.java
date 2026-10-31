package com.nagopy.android.overlayviewmanager.sample;

import android.accessibilityservice.AccessibilityService;
import android.app.Activity;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry;
import androidx.test.runner.lifecycle.Stage;
import androidx.test.uiautomator.UiDevice;

import com.nagopy.android.overlayviewmanager.OverlayFailure;
import com.nagopy.android.overlayviewmanager.OverlayResult;
import com.nagopy.android.overlayviewmanager.OverlaySpec;
import com.nagopy.android.overlayviewmanager.OverlayState;
import com.nagopy.android.overlayviewmanager.OverlayTouchMode;
import com.nagopy.android.overlayviewmanager.OverlayView;
import com.nagopy.android.overlayviewmanager.OverlayViewManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Proves on a device that a drag follows the finger: ACTION_DOWN leaves the window in place, and
 * each move displaces the window by exactly the gesture.
 *
 * Positions come from {@link View#getLocationOnScreen}, which reflects the frame the window
 * manager reported. Every case also drags back from the new position. That second ACTION_DOWN is
 * hit-tested by the system, so it only reaches the overlay if the window really is there.
 *
 * Unit tests cover the arithmetic with a supplied frame; this class covers the real framework
 * frames that they cannot.
 */
@RunWith(AndroidJUnit4.class)
public class DragGeometryTest {

    private static final String TAG = "DragGeometryTest";
    private static final int OVERLAY_SIZE_DP = 96;
    private static final int START_X_DP = 24;
    private static final int START_Y_DP = 48;
    private static final int GESTURE_STEPS = 3;
    private static final int TOLERANCE_PX = 1;
    private static final long DOWN_SETTLE_MS = 300;
    private static final long TIMEOUT_MS = 3000;
    private static final long POLL_MS = 20;

    @Rule
    public GrantOverlayPermissionRule grantPermissionRule = new GrantOverlayPermissionRule();

    @Rule
    public ActivityTestRule<SampleStartActivity> activityRule =
            new ActivityTestRule<>(SampleStartActivity.class);

    private Instrumentation instrumentation;
    private UiAutomation uiAutomation;
    private UiDevice uiDevice;
    private Activity host;
    private View target;
    private OverlayView<View> overlay;
    private boolean rotated;
    private boolean splitScreen;

    @Before
    public void setUp() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        uiAutomation = instrumentation.getUiAutomation();
        uiDevice = UiDevice.getInstance(instrumentation);
        host = activityRule.getActivity();
        uiDevice.waitForIdle();
        Log.i(TAG, "fingerprint=" + Build.FINGERPRINT);
    }

    @After
    public void tearDown() throws Exception {
        if (overlay != null) {
            instrumentation.runOnMainSync(() -> overlay.dispose());
        }
        GrantOverlayPermissionRule.setOverlayOp("allow");
        if (rotated) {
            uiDevice.setOrientationNatural();
            uiDevice.unfreezeRotation();
        }
        if (splitScreen) {
            uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN);
            uiDevice.waitForIdle();
        }
    }

    @Test
    public void activityScope_nonZeroFrameOrigin() {
        showActivityOverlay(newView(host), spec(false));
        assertTrue("A visible status bar gives the frame a non-zero origin: " + frame(),
                frame().top > 0 || frame().left > 0);
        assertDragAndReturnTrackGesture();
    }

    @Test
    public void applicationScope_nonZeroFrameOrigin() {
        showApplicationOverlay(newView(host.getApplicationContext()), spec(false));
        assertTrue("A visible status bar gives the frame a non-zero origin: " + frame(),
                frame().top > 0 || frame().left > 0);
        assertDragAndReturnTrackGesture();
    }

    @Test
    public void activityScope_rtl() {
        showActivityOverlay(newView(rtlContext(host)), spec(false));
        assertStartGravityResolvedToRightEdge();
        assertDragAndReturnTrackGesture();
    }

    @Test
    public void applicationScope_rtl() {
        showApplicationOverlay(newView(rtlContext(host.getApplicationContext())), spec(false));
        assertStartGravityResolvedToRightEdge();
        assertDragAndReturnTrackGesture();
    }

    @Test
    public void activityScope_edgeToEdgeHost() {
        layOutHostEdgeToEdge();
        showActivityOverlay(newView(host), spec(false));
        assertDragAndReturnTrackGesture();
    }

    @Test
    public void applicationScope_edgeToEdgeHost() {
        layOutHostEdgeToEdge();
        showApplicationOverlay(newView(host.getApplicationContext()), spec(false));
        assertDragAndReturnTrackGesture();
    }

    @Test
    public void activityScope_allowOutsideBounds_tracksPastTheScreenEdge() {
        showActivityOverlay(newView(host), spec(true));
        assertDragAndReturnTrackGesture();
        assertDragPastLeftEdgeTracksGesture();
    }

    @Test
    public void applicationScope_allowOutsideBounds_tracksPastTheScreenEdge() {
        showApplicationOverlay(newView(host.getApplicationContext()), spec(true));
        assertDragAndReturnTrackGesture();
        assertDragPastLeftEdgeTracksGesture();
    }

    @Test
    public void activityScope_outsideBoundsNotAllowed_staysOnScreen() {
        showActivityOverlay(newView(host), spec(false));
        assertDragPastLeftEdgeStaysOnScreen();
    }

    @Test
    public void applicationScope_outsideBoundsNotAllowed_staysOnScreen() {
        showApplicationOverlay(newView(host.getApplicationContext()), spec(false));
        assertDragPastLeftEdgeStaysOnScreen();
    }

    @Test
    public void applicationScope_afterRotation() throws Exception {
        showApplicationOverlay(newView(host.getApplicationContext()), spec(false));
        int naturalRotation = uiDevice.getDisplayRotation();
        rotated = true;
        uiDevice.setOrientationLeft();
        uiDevice.waitForIdle();
        assumeTrue("Rotation is not available on this device",
                uiDevice.getDisplayRotation() != naturalRotation);
        awaitStableLocation();
        assertDragAndReturnTrackGesture();
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    public void activityScope_inSplitScreen() {
        assumeTrue("Split screen is not available on this device", uiAutomation.performGlobalAction(
                AccessibilityService.GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN));
        splitScreen = true;
        Activity splitHost = awaitMultiWindowActivity();
        assumeTrue("The host did not enter split screen", splitHost != null);
        host = splitHost;
        showActivityOverlay(newView(host), spec(false));
        Log.i(TAG, "splitScreen frame=" + frame());
        assertDragAndReturnTrackGesture();
    }

    @Test
    public void activityScope_dragsWithoutOverlayPermission() throws Exception {
        GrantOverlayPermissionRule.setOverlayOp("deny");
        awaitOverlayPermission(false);

        showActivityOverlay(newView(host), spec(false));

        assertDragAndReturnTrackGesture();
    }

    @Test
    public void applicationScope_deniedThenGrantedThenShown() throws Exception {
        GrantOverlayPermissionRule.setOverlayOp("deny");
        awaitOverlayPermission(false);
        View view = newView(host.getApplicationContext());
        OverlayResult denied = onMain(() -> {
            overlay = OverlayViewManager.getInstance().newOverlayView(view, spec(false));
            return overlay.show();
        });
        assertEquals(OverlayFailure.PERMISSION_DENIED, denied.getFailure());
        assertEquals(OverlayState.CONFIGURED, denied.getState());

        GrantOverlayPermissionRule.setOverlayOp("allow");
        awaitOverlayPermission(true);
        assertShown(onMain(() -> overlay.show()));
        target = view;
        awaitAttached();
        assertDragAndReturnTrackGesture();

        // A hidden overlay comes back where the drag left it, still draggable.
        int[] draggedTo = locationOnScreen();
        assertTrue(onMain(() -> overlay.hide()).isSuccess());
        assertShown(onMain(() -> overlay.show()));
        awaitAttached();
        awaitLocation("Re-shown overlay", draggedTo[0], draggedTo[1]);
        assertDragAndReturnTrackGesture();
    }

    private OverlaySpec spec(boolean allowOutsideBounds) {
        return new OverlaySpec.Builder()
                .setWidth(dp(OVERLAY_SIZE_DP))
                .setHeight(dp(OVERLAY_SIZE_DP))
                .setGravity(Gravity.TOP | Gravity.START)
                .setX(dp(START_X_DP))
                .setY(dp(START_Y_DP))
                .setTouchMode(OverlayTouchMode.DRAGGABLE)
                .setAllowOutsideBounds(allowOutsideBounds)
                .build();
    }

    private View newView(Context context) {
        return onMain(() -> {
            View view = new View(context);
            view.setBackgroundColor(Color.MAGENTA);
            return view;
        });
    }

    /**
     * An RTL configuration, as a device in an RTL locale would give the view. The direction stays
     * inherited from the context, so nothing resolves it before the view is attached.
     */
    private static Context rtlContext(Context base) {
        Configuration configuration = new Configuration(base.getResources().getConfiguration());
        configuration.setLocale(new Locale("ar"));
        return base.createConfigurationContext(configuration);
    }

    private void showActivityOverlay(View view, OverlaySpec spec) {
        assertShown(onMain(() -> {
            overlay = OverlayViewManager.getInstance().newOverlayView(view, host, spec);
            return overlay.show();
        }));
        target = view;
        awaitAttached();
    }

    private void showApplicationOverlay(View view, OverlaySpec spec) {
        assertShown(onMain(() -> {
            overlay = OverlayViewManager.getInstance().newOverlayView(view, spec);
            return overlay.show();
        }));
        target = view;
        awaitAttached();
    }

    private void assertShown(OverlayResult result) {
        assertTrue("show() failed: " + result.getFailure() + " cause=" + result.getCause(),
                result.isSuccess());
        assertEquals(OverlayState.ATTACHED, result.getState());
    }

    private void layOutHostEdgeToEdge() {
        instrumentation.runOnMainSync(() -> host.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION));
        uiDevice.waitForIdle();
    }

    private void assertStartGravityResolvedToRightEdge() {
        int[] location = locationOnScreen();
        int width = displayMetrics().widthPixels;
        assertTrue("TOP|START under RTL belongs at the right edge, but left=" + location[0]
                        + " on a display " + width + "px wide",
                location[0] + dp(OVERLAY_SIZE_DP) / 2 > width / 2);
    }

    /** Drags toward the middle of the display, then back, so both window positions get hit-tested. */
    private void assertDragAndReturnTrackGesture() {
        int[] start = locationOnScreen();
        DisplayMetrics metrics = displayMetrics();
        int step = ViewConfiguration.get(host).getScaledTouchSlop() + dp(16);
        int size = dp(OVERLAY_SIZE_DP);
        int dx = (start[0] + size / 2 < metrics.widthPixels / 2 ? 1 : -1) * step * GESTURE_STEPS;
        int dy = (start[1] + size / 2 < metrics.heightPixels / 2 ? 1 : -1) * step * GESTURE_STEPS;

        assertDragTracksGesture(size / 2, size / 2, dx, dy);
        assertDragTracksGesture(size / 2, size / 2, -dx, -dy);
    }

    private void assertDragPastLeftEdgeTracksGesture() {
        int[] start = locationOnScreen();
        int touchOffsetX = dp(OVERLAY_SIZE_DP) - dp(8);
        int dx = dp(8) - (start[0] + touchOffsetX);

        assertDragTracksGesture(touchOffsetX, dp(OVERLAY_SIZE_DP) / 2, dx, 0);

        assertTrue("The window should extend past the left edge, left=" + locationOnScreen()[0],
                locationOnScreen()[0] < 0);
    }

    private void assertDragPastLeftEdgeStaysOnScreen() {
        int[] start = locationOnScreen();
        int touchOffsetX = dp(OVERLAY_SIZE_DP) - dp(8);
        int startX = start[0] + touchOffsetX;
        int y = start[1] + dp(OVERLAY_SIZE_DP) / 2;
        long downTime = SystemClock.uptimeMillis();
        inject(downTime, MotionEvent.ACTION_DOWN, startX, y);
        inject(downTime, MotionEvent.ACTION_MOVE, startX / 2, y);
        inject(downTime, MotionEvent.ACTION_MOVE, dp(8), y);
        inject(downTime, MotionEvent.ACTION_UP, dp(8), y);
        awaitStableLocation();

        int left = locationOnScreen()[0];
        assertTrue("Without allowOutsideBounds the window should stay on screen, left=" + left,
                left >= 0);
        assertTrue("The window should have moved to the frame's left edge, left=" + left
                        + ", start=" + start[0] + ", frame=" + frame(),
                left < start[0] && left <= frame().left + TOLERANCE_PX);
    }

    /**
     * Presses the overlay at the given offset from its corner and moves the pointer by (dx, dy) in
     * {@link #GESTURE_STEPS} moves. Each move must clear the touch slop, so that the first one
     * starts the drag and has to carry the whole displacement so far.
     */
    private void assertDragTracksGesture(int touchOffsetX, int touchOffsetY, int dx, int dy) {
        int slop = ViewConfiguration.get(host).getScaledTouchSlop();
        assertTrue("Each gesture step must exceed the touch slop",
                Math.max(Math.abs(dx), Math.abs(dy)) / GESTURE_STEPS >= slop);

        int[] origin = locationOnScreen();
        int startX = origin[0] + touchOffsetX;
        int startY = origin[1] + touchOffsetY;
        int lastX = startX;
        int lastY = startY;
        Log.i(TAG, "drag from=" + Arrays.toString(origin) + " by=(" + dx + "," + dy + ") frame="
                + frame() + " rotation=" + uiDevice.getDisplayRotation());

        long downTime = SystemClock.uptimeMillis();
        inject(downTime, MotionEvent.ACTION_DOWN, startX, startY);
        try {
            SystemClock.sleep(DOWN_SETTLE_MS);
            assertLocation("ACTION_DOWN must not move the window", origin[0], origin[1]);

            for (int i = 1; i <= GESTURE_STEPS; i++) {
                int movedX = dx * i / GESTURE_STEPS;
                int movedY = dy * i / GESTURE_STEPS;
                lastX = startX + movedX;
                lastY = startY + movedY;
                inject(downTime, MotionEvent.ACTION_MOVE, lastX, lastY);
                awaitLocation("Move " + i + "/" + GESTURE_STEPS, origin[0] + movedX, origin[1] + movedY);
            }
        } finally {
            inject(downTime, MotionEvent.ACTION_UP, lastX, lastY);
        }

        awaitLocation("After ACTION_UP", origin[0] + dx, origin[1] + dy);
        assertEquals("lastFailure=" + onMain(() -> overlay.getLastFailure()),
                OverlayState.ATTACHED, onMain(() -> overlay.getState()));
        assertEquals("The drag restores the spec's alpha", 1f, onMain(() -> overlay.getSpec().getAlpha()), 0f);
    }

    private void inject(long downTime, int action, int x, int y) {
        MotionEvent event = MotionEvent.obtain(
                downTime, SystemClock.uptimeMillis(), action, x, y, 0);
        event.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try {
            assertTrue("Could not inject " + MotionEvent.actionToString(action),
                    uiAutomation.injectInputEvent(event, true));
        } finally {
            event.recycle();
        }
    }

    private void awaitAttached() {
        long deadline = SystemClock.uptimeMillis() + TIMEOUT_MS;
        while (SystemClock.uptimeMillis() < deadline) {
            if (onMain(() -> target.isAttachedToWindow() && target.getWidth() > 0)) {
                awaitStableLocation();
                return;
            }
            SystemClock.sleep(POLL_MS);
        }
        fail("The overlay was not laid out");
    }

    private void awaitLocation(String description, int expectedX, int expectedY) {
        long deadline = SystemClock.uptimeMillis() + TIMEOUT_MS;
        while (SystemClock.uptimeMillis() < deadline) {
            int[] location = locationOnScreen();
            if (near(location[0], expectedX) && near(location[1], expectedY)) {
                return;
            }
            SystemClock.sleep(POLL_MS);
        }
        assertLocation(description, expectedX, expectedY);
    }

    private void assertLocation(String description, int expectedX, int expectedY) {
        int[] location = locationOnScreen();
        assertTrue(description + ": expected (" + expectedX + "," + expectedY + ") but was "
                        + Arrays.toString(location) + ", frame=" + frame(),
                near(location[0], expectedX) && near(location[1], expectedY));
    }

    private static boolean near(int actual, int expected) {
        return Math.abs(actual - expected) <= TOLERANCE_PX;
    }

    /** Waits until the window has stopped moving, for steps that have no exact expected position. */
    private void awaitStableLocation() {
        int[] previous = locationOnScreen();
        long deadline = SystemClock.uptimeMillis() + TIMEOUT_MS;
        while (SystemClock.uptimeMillis() < deadline) {
            SystemClock.sleep(DOWN_SETTLE_MS);
            int[] current = locationOnScreen();
            if (Arrays.equals(previous, current)) {
                return;
            }
            previous = current;
        }
    }

    private void awaitOverlayPermission(boolean granted) {
        long deadline = SystemClock.uptimeMillis() + TIMEOUT_MS;
        while (SystemClock.uptimeMillis() < deadline
                && Settings.canDrawOverlays(host) != granted) {
            SystemClock.sleep(POLL_MS);
        }
        if (granted) {
            assertTrue("The overlay permission was not granted", Settings.canDrawOverlays(host));
        } else {
            assertFalse("The overlay permission was not denied", Settings.canDrawOverlays(host));
        }
    }

    private Activity awaitMultiWindowActivity() {
        long deadline = SystemClock.uptimeMillis() + TIMEOUT_MS * 2;
        while (SystemClock.uptimeMillis() < deadline) {
            Activity activity = onMain(this::findMultiWindowActivity);
            if (activity != null) {
                return activity;
            }
            SystemClock.sleep(POLL_MS);
        }
        return null;
    }

    private Activity findMultiWindowActivity() {
        Collection<Activity> resumed =
                ActivityLifecycleMonitorRegistry.getInstance().getActivitiesInStage(Stage.RESUMED);
        for (Activity activity : resumed) {
            if (activity instanceof SampleStartActivity && activity.isInMultiWindowMode()) {
                return activity;
            }
        }
        return null;
    }

    private int[] locationOnScreen() {
        return onMain(() -> {
            int[] location = new int[2];
            target.getLocationOnScreen(location);
            return location;
        });
    }

    private Rect frame() {
        return onMain(() -> {
            Rect frame = new Rect();
            target.getWindowVisibleDisplayFrame(frame);
            return frame;
        });
    }

    private DisplayMetrics displayMetrics() {
        return target.getResources().getDisplayMetrics();
    }

    private int dp(int value) {
        return Math.round(value * host.getResources().getDisplayMetrics().density);
    }

    private <T> T onMain(MainCall<T> call) {
        AtomicReference<T> result = new AtomicReference<>();
        instrumentation.runOnMainSync(() -> result.set(call.run()));
        return result.get();
    }

    private interface MainCall<T> {
        T run();
    }
}
