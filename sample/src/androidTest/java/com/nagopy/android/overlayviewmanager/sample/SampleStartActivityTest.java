package com.nagopy.android.overlayviewmanager.sample;


import android.os.Build;
import android.support.test.espresso.Espresso;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;

import com.nagopy.android.overlayviewmanager.OverlayViewManager;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static android.support.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.junit.Assert.assertTrue;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SampleStartActivityTest {

    @Rule
    public ActivityTestRule<SampleStartActivity> mActivityTestRule = new ActivityTestRule<>(SampleStartActivity.class);

    @Before
    public void setup() {
        OverlayViewManager.init(mActivityTestRule.getActivity().getApplicationContext());
        assertTrue("Settings.canDrawOverlays(Context) returns false. Please allow first.",
                OverlayViewManager.canDrawOverlays());
    }

    @Test
    public void sampleStartActivityTest() throws Exception {
        Espresso.onIdle();
        Thread.sleep(500);
        ViewInteraction textView = onView(
                allOf(
                        withId(R.id.lbl_api_level)
                        , withText("API Level " + Build.VERSION.SDK_INT)
                )
        );
        textView.check(matches(withText("API Level " + Build.VERSION.SDK_INT)));
    }
}
