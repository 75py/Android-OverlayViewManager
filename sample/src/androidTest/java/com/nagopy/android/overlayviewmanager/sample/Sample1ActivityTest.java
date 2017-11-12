package com.nagopy.android.overlayviewmanager.sample;


import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewInteraction;
import android.support.test.espresso.matcher.RootMatchers;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;
import android.test.suitebuilder.annotation.LargeTest;

import com.nagopy.android.overlayviewmanager.OverlayViewManager;
import com.nagopy.android.overlayviewmanager.internal.ScreenMonitor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Random;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isClickable;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class Sample1ActivityTest {

    @Rule
    public ActivityTestRule<Sample1Activity> mActivityTestRule = new ActivityTestRule<>(Sample1Activity.class);

    private Sample1Activity activity;
    private UiDevice uiDevice;

    @Before
    public void setup() {
        uiDevice = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        activity = mActivityTestRule.getActivity();

        OverlayViewManager.init(activity.getApplicationContext());
        assertTrue("Settings.canDrawOverlays(Context) returns false. Please allow first.",
                OverlayViewManager.canDrawOverlays());
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
        ScreenMonitor.getInstance().cancelForce();

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
        ScreenMonitor.getInstance().cancelForce();

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
            ScreenMonitor.getInstance().cancelForce();

            int[] location = new int[2];
            activity.overlayView.getView().getLocationOnScreen(location);
            int w = activity.overlayView.getView().getMeasuredWidth();
            int h = activity.overlayView.getView().getMeasuredHeight();
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
    }
}
