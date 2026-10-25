package com.nagopy.android.overlayviewmanager.sample;

import android.app.Instrumentation;
import android.content.Context;
import android.content.Intent;
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
import android.widget.EditText;

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
 * The Settings app is the other UID and its search bar is the touch target: a delivered tap opens
 * the search screen's text field.
 */
@SdkSuppress(minSdkVersion = Build.VERSION_CODES.S)
@RunWith(AndroidJUnit4.class)
public class CrossUidPassThroughTest {

    private static final String TAG = "CrossUidPassThroughTest";
    private static final long TIMEOUT_MS = 5000;
    private static final long BLOCKED_OBSERVATION_MS = 3000;
    private static final float ALPHA_TOLERANCE = 0.005f;
    private static final Pattern DISPATCHED_ALPHA = Pattern.compile("[ ,]alpha=([0-9.]+)");

    @Rule
    public GrantOverlayPermissionRule grantPermissionRule = new GrantOverlayPermissionRule();

    private Instrumentation instrumentation;
    private UiDevice device;
    private Context context;
    private OverlayView<View> overlay;

    @Before
    public void setUp() {
        instrumentation = InstrumentationRegistry.getInstrumentation();
        device = UiDevice.getInstance(instrumentation);
        context = instrumentation.getTargetContext().getApplicationContext();
        Log.i(TAG, "Build fingerprint: " + Build.FINGERPRINT);
        context.startActivity(new Intent(Settings.ACTION_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK));
        assertNotNull("Settings search bar not found", findSearchBar());
        assertFalse("A text field is already shown", device.hasObject(By.clazz(EditText.class)));
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
        tapSearchBarThroughOverlay();
        assertTrue("The tap did not reach Settings",
                device.wait(Until.hasObject(By.clazz(EditText.class)), TIMEOUT_MS));
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
    public void opaqueSameUidOnly_followsThePlatformOpacityPolicy() throws IOException {
        // SAME_UID_ONLY only skips the library's opacity admission, so the platform decides. With
        // the opacity clamp the overlay is lowered to the maximum and the tap arrives; without it
        // the overlay stays opaque and the default untrusted-touch mode drops the tap.
        float maximum = maximumObscuringOpacity();
        assumeTrue("The device allows fully opaque pass-through", maximum < 1f);

        OverlayResult result = showFullScreenOverlay(1f, CrossUidPassThrough.SAME_UID_ONLY);

        assertTrue("show failed: " + result.getFailure(), result.isSuccess());
        float dispatched = dispatchedOverlayAlpha();
        Log.i(TAG, "Maximum obscuring opacity " + maximum + ", dispatched overlay alpha " + dispatched);
        if (dispatched <= maximum + ALPHA_TOLERANCE) {
            tapSearchBarThroughOverlay();
            assertTrue("The platform lowered the overlay to " + dispatched
                            + " but the tap did not reach Settings",
                    device.wait(Until.hasObject(By.clazz(EditText.class)), TIMEOUT_MS));
        } else {
            String mode = Settings.Global.getString(context.getContentResolver(), "block_untrusted_touches");
            assumeTrue("Untrusted touches are not in block mode: " + mode, mode == null || "2".equals(mode));
            tapSearchBarThroughOverlay();
            assertFalse("The platform delivered a tap through an overlay at alpha " + dispatched,
                    device.wait(Until.hasObject(By.clazz(EditText.class)), BLOCKED_OBSERVATION_MS));
        }
    }

    /**
     * Returns the overlay's alpha as the input dispatcher sees it, which is the value its cross-UID
     * opacity rule uses. The overlay is the only window of this UID: Settings is in front.
     */
    private float dispatchedOverlayAlpha() throws IOException {
        String owner = "ownerUid=" + Process.myUid() + ",";
        long deadline = SystemClock.uptimeMillis() + TIMEOUT_MS;
        do {
            for (String line : device.executeShellCommand("dumpsys input").split("\n")) {
                Matcher alpha = DISPATCHED_ALPHA.matcher(line);
                if (line.contains(owner) && alpha.find()) {
                    return Float.parseFloat(alpha.group(1));
                }
            }
            SystemClock.sleep(100);
        } while (SystemClock.uptimeMillis() < deadline);
        throw new AssertionError("No input window with " + owner + " in dumpsys input");
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
        device.waitForIdle();
        return result[0];
    }

    private void tapSearchBarThroughOverlay() {
        UiObject2 searchBar = findSearchBar();
        assertNotNull("Settings search bar not found under the overlay", searchBar);
        Point target = searchBar.getVisibleCenter();
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

    private UiObject2 findSearchBar() {
        BySelector byId = By.res(Pattern.compile("com\\.android\\.settings:id/search_(action_)?bar"));
        UiObject2 searchBar = device.wait(Until.findObject(byId), TIMEOUT_MS);
        if (searchBar == null) {
            searchBar = device.findObject(By.text(Pattern.compile("Search [Ss]ettings")));
        }
        return searchBar;
    }
}
