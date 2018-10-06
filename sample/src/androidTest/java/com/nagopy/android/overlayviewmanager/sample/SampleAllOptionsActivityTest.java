/*
 * Copyright 2017 75py
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nagopy.android.overlayviewmanager.sample;


import android.Manifest;
import android.content.res.ColorStateList;
import android.graphics.Color;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.ViewAction;
import androidx.test.espresso.ViewInteraction;
import androidx.test.espresso.action.CoordinatesProvider;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;
import androidx.test.runner.AndroidJUnit4;
import androidx.cardview.widget.CardView;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.SeekBar;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class SampleAllOptionsActivityTest {

    @Rule
    public GrantPermissionRule grantPermissionRule = GrantPermissionRule.grant(Manifest.permission.SYSTEM_ALERT_WINDOW);

    @Rule
    public ActivityTestRule<SampleAllOptionsActivity> mActivityTestRule = new ActivityTestRule<>(SampleAllOptionsActivity.class);
    private SampleAllOptionsActivity activity;
    private ColorStateList defaultCardColor;

    @Before
    public void setup() {
        activity = mActivityTestRule.getActivity();
        defaultCardColor = activity.binding.parentFlags.getCardBackgroundColor();

        waitALittle(1000);
    }

    @After
    public void teardown() {
        waitALittle(5000);
    }

    private void waitALittle() {
        waitALittle(1000);
    }

    private void waitALittle(long ms) {
        try {
            Espresso.onIdle();
            Thread.sleep(ms);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void performAndWait(final ViewInteraction viewInteraction, final ViewAction... viewActions) {
        viewInteraction.perform(viewActions);
        waitALittle();
    }

    private void updateActiveCard(final CardView active) {
        waitALittle(1000);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activity.binding.parentFlags.setCardBackgroundColor(defaultCardColor);
                activity.binding.parentWidth.setCardBackgroundColor(defaultCardColor);
                activity.binding.parentHeight.setCardBackgroundColor(defaultCardColor);
                activity.binding.parentXy.setCardBackgroundColor(defaultCardColor);
                activity.binding.parentAlpha.setCardBackgroundColor(defaultCardColor);
                activity.binding.parentGravity.setCardBackgroundColor(defaultCardColor);
                activity.binding.parentMargins.setCardBackgroundColor(defaultCardColor);

                activity.binding.scrollView.smoothScrollTo(0, active.getBottom() - active.getMeasuredHeight());
                active.setCardBackgroundColor(Color.argb(100, 255, 255, 0));
                switch (active.getId()) {
                    case R.id.parent_flags:
                        break;
                    case R.id.parent_width:
                        activity.overlayView.getView().setText("OverlayView#setWidth(int)");
                        break;
                    case R.id.parent_height:
                        activity.overlayView.getView().setText("OverlayView#setHeight(int)");
                        break;
                    case R.id.parent_xy:
                        activity.overlayView.getView().setText("OverlayView#setX(int), setY(int)");
                        break;
                    case R.id.parent_alpha:
                        activity.overlayView.getView().setText("OverlayView#setAlpha(float)");
                        break;
                    case R.id.parent_gravity:
                        activity.overlayView.getView().setText("OverlayView#setGravity(int)");
                        break;
                    case R.id.parent_margins:
                        activity.overlayView.getView().setText("OverlayView#setVerticalMargin(float), setHorizontalMargin(float)");
                        break;
                }
            }
        });
        waitALittle(1000);
    }

    @Test
    public void sampleAllOptionsActivityTest() {
        waitALittle();

        boolean skipWidth = true;
        boolean skipHeight = true;
        boolean skipXy = true;
        boolean skipAlpha = true;
        boolean skipGravity = false;
        boolean skipMargins = false;

        // Click "show()"
        ViewInteraction showButton = onView(withId(R.id.btn_show));
        showButton.check(ViewAssertions.matches(withText("show()")));
        performAndWait(showButton, scrollTo(), click());

        // Width
        if (!skipWidth) {
            updateActiveCard(activity.binding.parentWidth);

            ViewInteraction rbtnWidthWc = onView(
                    allOf(withId(R.id.rbtn_width_wc),
                            withText("WRAP_CONTENT")));
            performAndWait(rbtnWidthWc, click());

            ViewInteraction rbtnWidthMp = onView(
                    allOf(withId(R.id.rbtn_width_mp),
                            withText("MATCH_PARENT")));
            performAndWait(rbtnWidthMp, click());

            ViewInteraction rbtnWidthPx = onView(
                    allOf(withId(R.id.rbtn_width_px),
                            withText("Pixels")));
            performAndWait(rbtnWidthPx, click());

            ViewInteraction seekWidthPx = onView(withId(R.id.seek_width_px));
            for (int i = 400; i < 1000; i += 100) {
                performAndWait(seekWidthPx, clickSeekBar(i));
            }

            performAndWait(rbtnWidthWc, click());
        }

        // Height
        if (!skipHeight) {
            updateActiveCard(activity.binding.parentHeight);

            ViewInteraction rbtnWidthWc = onView(
                    allOf(withId(R.id.rbtn_height_wc),
                            withText("WRAP_CONTENT")));
            performAndWait(rbtnWidthWc, click());

            ViewInteraction rbtnHeightMp = onView(
                    allOf(withId(R.id.rbtn_height_mp),
                            withText("MATCH_PARENT")));
            performAndWait(rbtnHeightMp, click());

            ViewInteraction rbtnHeightPx = onView(
                    allOf(withId(R.id.rbtn_height_px),
                            withText("Pixels")));
            performAndWait(rbtnHeightPx, click());

            ViewInteraction seekHeightPx = onView(withId(R.id.seek_height_px));
            for (int i = 400; i < 1000; i += 100) {
                performAndWait(seekHeightPx, clickSeekBar(i));
            }

            ViewInteraction rbtnHeightWc = onView(
                    allOf(withId(R.id.rbtn_height_wc),
                            withText("WRAP_CONTENT")));
            performAndWait(rbtnHeightWc, click());
        }

        // X,Y
        if (!skipXy) {
            updateActiveCard(activity.binding.parentXy);
            int displayWidth = activity.binding.getDisplayWidth();
            int displayHeight = activity.binding.getDisplayHeight();

            ViewInteraction seekX = onView(withId(R.id.seek_x));
            for (int i = 0; i <= 500; i += 100) {
                performAndWait(seekX, clickSeekBar(i + displayWidth));
            }
            performAndWait(seekX, clickSeekBar(displayWidth));

            ViewInteraction seekY = onView(withId(R.id.seek_y));
            for (int i = 0; i <= 500; i += 100) {
                performAndWait(seekY, clickSeekBar(i + displayHeight));
            }
            performAndWait(seekY, clickSeekBar(displayHeight));
        }

        // Alpha
        if (!skipAlpha) {
            updateActiveCard(activity.binding.parentAlpha);

            ViewInteraction seekAlpha = onView(withId(R.id.seek_alpha));
            for (int i = 80; i >= 0; i -= 20) {
                performAndWait(seekAlpha, clickSeekBar(i));
            }
            performAndWait(seekAlpha, clickSeekBar(90));
        }

        // Gravity
        if (!skipGravity) {
            updateActiveCard(activity.binding.parentGravity);

            ViewInteraction rbtnTopStart = onView(
                    allOf(withId(R.id.rbtn_top_start),
                            withText("Gravity.TOP | Gravity.START")));
            performAndWait(rbtnTopStart, scrollTo(), click());

            ViewInteraction rbtnTopCenterVertical = onView(
                    allOf(withId(R.id.rbtn_top_center_vertical),
                            withText("Gravity.TOP | Gravity.CENTER_VERTICAL")));
            performAndWait(rbtnTopCenterVertical, scrollTo(), click());

            ViewInteraction rbtnTopEnd = onView(
                    allOf(withId(R.id.rbtn_top_end),
                            withText("Gravity.TOP | Gravity.END")));
            performAndWait(rbtnTopEnd, scrollTo(), click());

            ViewInteraction rbtnCenterStart = onView(
                    allOf(withId(R.id.rbtn_center_start),
                            withText("Gravity.CENTER_VERTICAL | Gravity.START")));
            performAndWait(rbtnCenterStart, scrollTo(), click());

            ViewInteraction rbtnCenter = onView(
                    allOf(withId(R.id.rbtn_center),
                            withText("Gravity.CENTER")));
            performAndWait(rbtnCenter, scrollTo(), click());

            ViewInteraction rbtnCenterEnd = onView(
                    allOf(withId(R.id.rbtn_center_end),
                            withText("Gravity.CENTER_VERTICAL | Gravity.END")));
            performAndWait(rbtnCenterEnd, scrollTo(), click());

            ViewInteraction rbtnBottomStart = onView(
                    allOf(withId(R.id.rbtn_bottom_start),
                            withText("Gravity.BOTTOM | Gravity.START")));
            performAndWait(rbtnBottomStart, scrollTo(), click());

            ViewInteraction rbtnBottomCenterVertical = onView(
                    allOf(withId(R.id.rbtn_bottom_center_vertical),
                            withText("Gravity.BOTTOM | Gravity.CENTER_VERTICAL")));
            performAndWait(rbtnBottomCenterVertical, scrollTo(), click());

            ViewInteraction rbtnBottomEnd = onView(
                    allOf(withId(R.id.rbtn_bottom_end),
                            withText("Gravity.BOTTOM | Gravity.END")));
            performAndWait(rbtnBottomEnd, scrollTo(), click());

            performAndWait(rbtnTopStart, scrollTo(), click());
        }

        // Margins
        if (!skipMargins) {
            updateActiveCard(activity.binding.parentMargins);

            ViewInteraction seekMarginVertical = onView(withId(R.id.seek_margin_vertical));
            for (int i = 0; i <= 100; i += 20) {
                performAndWait(seekMarginVertical, scrollTo(), clickSeekBar(i));
            }
            performAndWait(seekMarginVertical, scrollTo(), clickSeekBar(0));

            ViewInteraction seekMarginHorizontal = onView(withId(R.id.seek_margin_horizontal));
            for (int i = 0; i <= 15; i += 3) {
                performAndWait(seekMarginHorizontal, scrollTo(), clickSeekBar(i));
            }
            performAndWait(seekMarginHorizontal, scrollTo(), clickSeekBar(0));
        }
    }

    // https://qiita.com/yakitorizanmai/items/07c730db6bfccd5ff95f
    private ViewAction clickSeekBar(final int pos) {
        return new GeneralClickAction(
                Tap.SINGLE,
                new CoordinatesProvider() {
                    @Override
                    public float[] calculateCoordinates(View view) {
                        SeekBar seekBar = (SeekBar) view;
                        final int[] screenPos = new int[2];
                        seekBar.getLocationOnScreen(screenPos);

                        int trueWidth = seekBar.getWidth()
                                - seekBar.getPaddingLeft() - seekBar.getPaddingRight();

                        float relativePos = (0.3f + pos) / (float) seekBar.getMax();
                        if (relativePos > 1.0f)
                            relativePos = 1.0f;

                        final float screenX = trueWidth * relativePos + screenPos[0]
                                + seekBar.getPaddingLeft();
                        final float screenY = seekBar.getHeight() / 2f + screenPos[1];

                        return new float[]{screenX, screenY};
                    }
                },
                Press.FINGER, InputDevice.SOURCE_MOUSE, MotionEvent.BUTTON_PRIMARY);
    }
}
