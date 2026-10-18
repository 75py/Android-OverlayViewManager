package com.nagopy.android.overlayviewmanager.sample;


import android.os.Build;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewInteraction;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;


import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@RunWith(AndroidJUnit4.class)
public class SampleStartActivityTest {

    @Rule
    public GrantOverlayPermissionRule grantPermissionRule = new GrantOverlayPermissionRule();

    @Rule
    public ActivityTestRule<SampleStartActivity> mActivityTestRule = new ActivityTestRule<>(SampleStartActivity.class);

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
