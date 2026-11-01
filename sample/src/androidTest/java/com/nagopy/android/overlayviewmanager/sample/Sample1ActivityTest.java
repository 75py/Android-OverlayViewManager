package com.nagopy.android.overlayviewmanager.sample;


import android.os.Build;
import android.view.View;
import android.widget.TextView;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.matcher.RootMatchers;
import androidx.test.filters.SdkSuppress;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;

import com.nagopy.android.overlayviewmanager.OverlaySpec;
import com.nagopy.android.overlayviewmanager.OverlayState;
import com.nagopy.android.overlayviewmanager.OverlayTouchMode;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isClickable;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN_MR2)
@RunWith(AndroidJUnit4.class)
public class Sample1ActivityTest {

    @Rule
    public GrantOverlayPermissionRule grantPermissionRule = new GrantOverlayPermissionRule();

    @Rule
    public ActivityTestRule<Sample1Activity> mActivityTestRule = new ActivityTestRule<>(Sample1Activity.class);

    private Sample1Activity activity;
    private UiDevice uiDevice;

    @Before
    public void setup() {
        // SampleApplication already initialized OverlayViewManager on the main thread; init() is
        // main-thread only, so the instrumentation thread must not call it again here.
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        activity = mActivityTestRule.getActivity();
    }

    private void waitALittle() {
        try {
            Espresso.onIdle();
            uiDevice.waitForIdle(500);
            Thread.sleep(500);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void sampleStartActivityTest() throws Exception {
        waitALittle();

        ViewInteraction button = onView(
                allOf(withId(R.id.button), isDisplayed())
        );
        button.check(matches(isDisplayed()));
        waitALittle();

        ViewInteraction webView = onView(
                allOf(withId(R.id.webView), isDisplayed())
        );
        webView.check(matches(isDisplayed()));

        ViewInteraction appCompatButton2 = onView(
                allOf(withId(R.id.button), withText("Show / Hide"), isDisplayed())
        );
        appCompatButton2.perform(click());

        waitALittle();
        // state is a thread-safe read: the show() requested by the button succeeded. lastFailure
        // is included so a failing run explains why the window was not attached.
        assertEquals("lastFailure=" + activity.getOverlayView().getLastFailure(),
                OverlayState.ATTACHED, activity.getOverlayView().getState());
        ViewInteraction textView4 = onView(
                allOf(withId(R.id.sample_text_view), withText("click:0"), isDisplayed()))
                .inRoot(RootMatchers.withDecorView(not(is(activity.getWindow().getDecorView()))));
        textView4.check(matches(allOf(
                isDisplayed()
                , withText("click:0")
                , isClickable()
        )));
        textView4.perform(click());

        waitALittle();
        ViewInteraction textView6 = onView(
                allOf(withId(R.id.sample_text_view), withText("click:1"), isDisplayed()))
                .inRoot(RootMatchers.withDecorView(not(is(activity.getWindow().getDecorView()))));
        textView6.check(matches(allOf(
                isDisplayed()
                , withText("click:1")
                , isClickable()
        )));

        for (int i = 0; i < 10; i++) {
            waitALittle();
            int[] location = new int[2];
            int[] size = new int[2];
            // OverlayView.getView() is main-thread only.
            InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
                TextView view = activity.getOverlayView().getView();
                view.getLocationOnScreen(location);
                size[0] = view.getMeasuredWidth();
                size[1] = view.getMeasuredHeight();
            });
            int w = size[0];
            int h = size[1];
            int startX = location[0] + w / 2;
            int startY = location[1] + h / 2;
            int step = (int) (100 * new Random().nextDouble() + 50);
            int endX, endY;
            switch (i) {
                case 0:
                    endX = uiDevice.getDisplayWidth();
                    endY = uiDevice.getDisplayHeight();
                    break;
                case 1:
                    endX = 0;
                    endY = 0;
                    break;
                case 2:
                    endX = uiDevice.getDisplayWidth() / 2;
                    endY = uiDevice.getDisplayHeight();
                    break;
                case 3:
                    endX = uiDevice.getDisplayWidth() / 2;
                    endY = uiDevice.getDisplayHeight() / 2;
                    break;
                case 4:
                    endX = uiDevice.getDisplayWidth();
                    endY = uiDevice.getDisplayHeight() / 2;
                    break;
                case 5:
                case 6:
                case 7:
                case 8:
                case 9:
                default:
                    endX = (int) (uiDevice.getDisplayWidth() * new Random().nextDouble());
                    endY = (int) (uiDevice.getDisplayHeight() * new Random().nextDouble());
                    break;
            }
            uiDevice.drag(startX, startY, endX, endY, step);
        }
        // Dragging moves the window but never detaches it.
        assertEquals("lastFailure=" + activity.getOverlayView().getLastFailure(),
                OverlayState.ATTACHED, activity.getOverlayView().getState());
    }

    @Test
    public void hideThenShow_reattaches() {
        waitALittle();

        ViewInteraction button = onView(allOf(withId(R.id.button), isDisplayed()));
        button.perform(click());
        waitALittle();
        assertEquals("lastFailure=" + activity.getOverlayView().getLastFailure(),
                OverlayState.ATTACHED, activity.getOverlayView().getState());

        button.perform(click());
        waitALittle();
        assertEquals("lastFailure=" + activity.getOverlayView().getLastFailure(),
                OverlayState.CONFIGURED, activity.getOverlayView().getState());

        button.perform(click());
        waitALittle();
        assertEquals("lastFailure=" + activity.getOverlayView().getLastFailure(),
                OverlayState.ATTACHED, activity.getOverlayView().getState());
    }

    @Test
    public void activityRecreated_rebuildsAndShowsOverlay() throws Exception {
        waitALittle();

        onView(allOf(withId(R.id.button), isDisplayed())).perform(click());
        waitALittle();
        assertEquals(OverlayState.ATTACHED, activity.getOverlayView().getState());

        activity.runOnUiThread(() -> activity.recreate());
        waitALittle();
        Sample1Activity recreated = mActivityTestRule.getActivity();
        // The recreated Activity owns a fresh, unattached handle.
        assertEquals(OverlayState.CONFIGURED, recreated.getOverlayView().getState());

        onView(allOf(withId(R.id.button), isDisplayed())).perform(click());
        waitALittle();
        assertEquals("lastFailure=" + recreated.getOverlayView().getLastFailure(),
                OverlayState.ATTACHED, recreated.getOverlayView().getState());
    }

    @Test
    public void passThroughTouchMode_deliversClickToWindowBelow() {
        waitALittle();

        onView(allOf(withId(R.id.button), isDisplayed())).perform(click());
        waitALittle();
        assertEquals(OverlayState.ATTACHED, activity.getOverlayView().getState());

        // Center the overlay on the toggle button so the same screen point is inside both.
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View overlay = activity.getOverlayView().getView();
            View button = activity.findViewById(R.id.button);
            int[] overlayPos = new int[2];
            int[] buttonPos = new int[2];
            overlay.getLocationOnScreen(overlayPos);
            button.getLocationOnScreen(buttonPos);
            OverlaySpec current = activity.getOverlayView().getSpec();
            OverlaySpec spec = current.toBuilder()
                    .setX(buttonPos[0] + button.getWidth() / 2 - overlay.getMeasuredWidth() / 2
                            - overlayPos[0] + current.getX())
                    .setY(buttonPos[1] + button.getHeight() / 2 - overlay.getMeasuredHeight() / 2
                            - overlayPos[1] + current.getY())
                    .build();
            activity.getOverlayView().update(spec);
        });
        waitALittle();

        int[] center = new int[2];
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> {
            View overlay = activity.getOverlayView().getView();
            int[] overlayPos = new int[2];
            overlay.getLocationOnScreen(overlayPos);
            center[0] = overlayPos[0] + overlay.getMeasuredWidth() / 2;
            center[1] = overlayPos[1] + overlay.getMeasuredHeight() / 2;
        });

        // In DRAGGABLE mode the overlay itself consumes the tap as a click.
        uiDevice.click(center[0], center[1]);
        waitALittle();
        assertEquals(OverlayState.ATTACHED, activity.getOverlayView().getState());
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                assertEquals("click:1", ((TextView) activity.getOverlayView().getView()).getText().toString()));

        // PASS_THROUGH mode lets the same tap reach the button below and hide the overlay.
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() ->
                activity.getOverlayView().update(activity.getOverlayView().getSpec().toBuilder()
                        .setTouchMode(OverlayTouchMode.PASS_THROUGH).build()));
        waitALittle();
        uiDevice.click(center[0], center[1]);
        waitALittle();
        assertEquals("lastFailure=" + activity.getOverlayView().getLastFailure(),
                OverlayState.CONFIGURED, activity.getOverlayView().getState());
    }
}
