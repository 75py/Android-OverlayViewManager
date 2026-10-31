package com.nagopy.android.overlayviewmanager.sample;

import android.app.Instrumentation;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.input.InputManager;
import android.os.Build;
import android.os.Process;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import androidx.test.filters.SdkSuppress;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.BySelector;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import com.nagopy.android.overlayviewmanager.CrossUidPassThrough;
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

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

/**
 * Device check for application overlays in {@link OverlayTouchMode#PASS_THROUGH} above another
 * application's window. Since Android 12 the input dispatcher drops touches that pass through a
 * window owned by a different UID unless that window's opacity is at most
 * {@link InputManager#getMaximumObscuringOpacityForTouch()}. Builds with the window manager's
 * opacity clamp (an AOSP change from March 2022, absent from the initial Android 12 release) lower a
 * non-touchable application overlay above that maximum to the maximum instead.
 *
 * <p>The touch target is {@link TapReceiverActivity}, which runs with the test APK's UID. System
 * apps such as Settings are unsuitable: they hide non-system overlays while in front, so a tap
 * reaches them without passing through the overlay at all.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
@RunWith(AndroidJUnit4.class)
public class CrossUidPassThroughTest {

    private static final String TAG = "CrossUidPassThroughTest";
    private static final long TIMEOUT_MS = 5000;
    private static final long BLOCKED_OBSERVATION_MS = 3000;
    private static final float ALPHA_TOLERANCE = 0.005f;
    private static final Pattern DISPATCHED_ALPHA = Pattern.compile("[ ,]alpha=([0-9.]+)");
    /** {@code TYPE_APPLICATION_OVERLAY} as API 31 prints a window type in {@code dumpsys input}. */
    private static final Pattern OVERLAY_TYPE =
            Pattern.compile("[ ,]type=(APPLICATION_OVERLAY|2038|0x0*7f6)(,|\\s|$)");
    private static final Pattern ANY_TYPE = Pattern.compile("[ ,]type=");

    @Rule
    public GrantOverlayPermissionRule grantPermissionRule = new GrantOverlayPermissionRule();

    private Instrumentation instrumentation;
    private UiDevice device;
    private Context context;
    private OverlayView<View> overlay;
    private float dispatchedOverlayAlpha = Float.NaN;
    private BySelector receiver;

    @Before
    public void setUp() throws IOException {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        context = instrumentation.getTargetContext().getApplicationContext();
        Log.i(TAG, "Build fingerprint: " + Build.FINGERPRINT);

        String receiverPackage = instrumentation.getContext().getPackageName();
        int receiverUid = instrumentation.getContext().getApplicationInfo().uid;
        assertNotEquals("The receiver must not share the sample app's UID",
                context.getApplicationInfo().uid, receiverUid);
        String started = device.executeShellCommand("am start -W --activity-clear-task -n "
                + receiverPackage + "/" + TapReceiverActivity.class.getName());
        Log.i(TAG, "Tap receiver start: " + started.trim());
        assertNotNull("Tap receiver not shown", device.wait(Until.findObject(
                By.pkg(receiverPackage).text(TapReceiverActivity.TAPS_PREFIX + 0)), TIMEOUT_MS));
        UiObject2 uid = device.findObject(By.pkg(receiverPackage)
                .text(Pattern.compile(Pattern.quote(TapReceiverActivity.UID_PREFIX) + "\\d+")));
        assertNotNull("Tap receiver UID not shown", uid);
        assertEquals("The receiver runs with an unexpected UID",
                TapReceiverActivity.UID_PREFIX + receiverUid, uid.getText());
        assertNotEquals("The receiver runs in the sample app's UID",
                TapReceiverActivity.UID_PREFIX + Process.myUid(), uid.getText());
        receiver = By.pkg(receiverPackage);
    }

    @After
    public void tearDown() {
        if (overlay != null) {
            instrumentation.runOnMainSync(() -> overlay.dispose());
        }
        device.pressHome();
    }

    @Test
    public void belowMaximumOpacity_whenSystemAllows_deliversTapToOtherApp() {
        float alpha = maximumObscuringOpacity() / 2f;

        OverlayResult result = showFullScreenOverlay(alpha, CrossUidPassThrough.WHEN_SYSTEM_ALLOWS);

        assertTrue("show failed: " + result.getFailure(), result.isSuccess());
        tapReceiverThroughOverlay();
        assertTrue("The tap did not reach the receiver", awaitTaps(1, TIMEOUT_MS));
    }

    @Test
    public void aboveMaximumOpacity_whenSystemAllows_isRejectedBeforeAttach() {
        float maximum = maximumObscuringOpacity();
        assumeTrue("The device allows fully opaque pass-through", maximum < 1f);
        float alpha = Math.min(1f, maximum + (1f - maximum) / 2f);

        OverlayResult result = showFullScreenOverlay(alpha, CrossUidPassThrough.WHEN_SYSTEM_ALLOWS);

        assertFalse(result.isSuccess());
        assertEquals(OverlayFailure.PASS_THROUGH_OPACITY_EXCEEDED, result.getFailure());
        assertEquals(OverlayState.CONFIGURED, overlay.getState());
    }

    @Test
    public void opaqueSameUidOnly_followsThePlatformOpacityPolicy() {
        // SAME_UID_ONLY only skips the library's opacity admission, so the platform decides. With
        // the opacity clamp the overlay is lowered to the maximum and the tap arrives; without it
        // the overlay stays opaque and the default untrusted-touch mode drops the tap.
        float maximum = maximumObscuringOpacity();
        assumeTrue("The device allows fully opaque pass-through", maximum < 1f);

        OverlayResult result = showFullScreenOverlay(1f, CrossUidPassThrough.SAME_UID_ONLY);

        assertTrue("show failed: " + result.getFailure(), result.isSuccess());
        float dispatched = dispatchedOverlayAlpha;
        Log.i(TAG, "Maximum obscuring opacity " + maximum + ", dispatched overlay alpha " + dispatched);
        if (dispatched <= maximum + ALPHA_TOLERANCE) {
            tapReceiverThroughOverlay();
            assertTrue("The platform lowered the overlay to " + dispatched
                    + " but the tap did not reach the receiver", awaitTaps(1, TIMEOUT_MS));
        } else {
            String mode = Settings.Global.getString(context.getContentResolver(), "block_untrusted_touches");
            assumeTrue("Untrusted touches are not in block mode: " + mode, mode == null || "2".equals(mode));
            tapReceiverThroughOverlay();
            assertFalse("The platform delivered a tap through an overlay at alpha " + dispatched,
                    awaitTaps(1, BLOCKED_OBSERVATION_MS));
            assertTrue("The receiver's tap count changed unexpectedly",
                    device.hasObject(By.copy(receiver).text(TapReceiverActivity.TAPS_PREFIX + 0)));
        }
    }

    /**
     * Waits until the input dispatcher lists the overlay as a visible application overlay of this
     * UID and returns its alpha, the value the cross-UID opacity rule uses. Other lines of this UID,
     * such as an invisible layer of unknown type, can precede it. The overlay is also listed as
     * invisible while a window in front hides non-system overlays, in which case a tap never passes
     * through it. API 31 prints {@code visible=} and {@code type=}; later APIs print
     * {@code inputConfig=} flags, where {@code NOT_VISIBLE} marks an invisible window.
     */
    private float awaitOverlayInputWindowAlpha() throws IOException {
        String owner = "ownerUid=" + Process.myUid() + ",";
        long deadline = SystemClock.uptimeMillis() + TIMEOUT_MS;
        String[] lines;
        do {
            lines = device.executeShellCommand("dumpsys input").split("\n");
            for (String line : lines) {
                Matcher alpha = DISPATCHED_ALPHA.matcher(line);
                if (line.contains(owner) && isReadyOverlayInputWindow(line) && alpha.find()) {
                    Log.i(TAG, "Overlay input window: " + line.trim());
                    return Float.parseFloat(alpha.group(1));
                }
            }
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() < deadline);
        for (String line : lines) {
            if (line.contains(owner)) {
                Log.i(TAG, "Input window of this UID: " + line.trim());
            }
        }
        throw new AssertionError("No visible overlay input window with " + owner + " in dumpsys input");
    }

    private static boolean isReadyOverlayInputWindow(String line) {
        if (line.contains("visible=") && !line.contains("visible=true")) {
            return false;
        }
        if (line.contains("inputConfig=")
                && (line.contains("NOT_VISIBLE") || !line.contains("NOT_TOUCHABLE"))) {
            return false;
        }
        return !ANY_TYPE.matcher(line).find() || OVERLAY_TYPE.matcher(line).find();
    }

    private float maximumObscuringOpacity() {
        return context.getSystemService(InputManager.class).getMaximumObscuringOpacityForTouch();
    }

    private OverlayResult showFullScreenOverlay(float alpha, CrossUidPassThrough crossUid) {
        OverlayResult[] result = new OverlayResult[1];
        instrumentation.runOnMainSync(() -> {
            View view = new View(context);
            view.setBackgroundColor(Color.MAGENTA);
            OverlaySpec spec = new OverlaySpec.Builder()
                    .setWidth(ViewGroup.LayoutParams.MATCH_PARENT)
                    .setHeight(ViewGroup.LayoutParams.MATCH_PARENT)
                    .setTouchMode(OverlayTouchMode.PASS_THROUGH)
                    .setCrossUidPassThrough(crossUid)
                    .setAlpha(alpha)
                    .build();
            overlay = OverlayViewManager.getInstance().newOverlayView(view, spec);
            result[0] = overlay.show();
        });
        instrumentation.waitForIdleSync();
        device.waitForIdle();
        if (result[0].isSuccess()) {
            try {
                dispatchedOverlayAlpha = awaitOverlayInputWindowAlpha();
            } catch (IOException exception) {
                throw new AssertionError("dumpsys input failed", exception);
            }
        }
        return result[0];
    }

    private void tapReceiverThroughOverlay() {
        UiObject2 tapArea = device.findObject(By.copy(receiver).desc(TapReceiverActivity.TAP_AREA_DESCRIPTION));
        assertNotNull("Tap receiver not found under the overlay", tapArea);
        Point target = tapArea.getVisibleCenter();
        boolean[] covered = new boolean[1];
        instrumentation.runOnMainSync(() -> {
            View view = overlay.getView();
            int[] location = new int[2];
            view.getLocationOnScreen(location);
            covered[0] = view.isAttachedToWindow()
                    && target.x >= location[0] && target.x < location[0] + view.getWidth()
                    && target.y >= location[1] && target.y < location[1] + view.getHeight();
        });
        assertTrue("The overlay does not cover the tap target " + target, covered[0]);
        device.click(target.x, target.y);
    }

    /** Returns whether the receiver shows exactly {@code taps} taps within {@code timeoutMs}. */
    private boolean awaitTaps(int taps, long timeoutMs) {
        return device.wait(Until.hasObject(By.copy(receiver).text(TapReceiverActivity.TAPS_PREFIX + taps)),
                timeoutMs);
    }
}
