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

import android.annotation.SuppressLint;
import android.databinding.DataBindingUtil;
import android.databinding.adapters.SeekBarBindingAdapter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import com.nagopy.android.overlayviewmanager.OverlayView;
import com.nagopy.android.overlayviewmanager.OverlayViewManager;
import com.nagopy.android.overlayviewmanager.sample.databinding.ActivitySampleAllOptionsBinding;

import java.text.SimpleDateFormat;
import java.util.Date;

import timber.log.Timber;

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

// file:///Users/nagopy/workspace/201710//OverlayViewManager/library/build/reports/jacoco/jacocoTestDebugUnitTestReport/html/com.nagopy.android.overlayviewmanager/DraggableOnTouchListener.java.html#L24
// file:///Users/nagopy/workspace/201710/OverlayViewManager/library/build/tmp/index.html
@SuppressLint("SetOverlayAboveNavigationViews")
public class SampleAllOptionsActivity extends AppCompatActivity implements
        CheckBox.OnCheckedChangeListener
        , View.OnClickListener
        , SeekBarBindingAdapter.OnProgressChanged
        , RadioGroup.OnCheckedChangeListener {

    ActivitySampleAllOptionsBinding binding;
    OverlayView<TextView> overlayView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("onCreate %s", savedInstanceState);

        overlayView = OverlayView.create(createTextView())
                .setAlpha(0.9f)
                .setOnClickListener(this);

        binding = DataBindingUtil.setContentView(this, R.layout.activity_sample_all_options);

        binding.setOnCheckedChangeListener(this);
        binding.setOnProgressChanged(this);
        binding.setOnRadioCheckedChangeListener(this);
        binding.setDisplayWidth(OverlayViewManager.getDisplayWidth());
        binding.setDisplayHeight(OverlayViewManager.getDisplayHeight());
        if (savedInstanceState == null) {
            binding.xy.setXSeek(OverlayViewManager.getDisplayWidth()); // Seekbar progress = 0
            binding.xy.setYSeek(OverlayViewManager.getDisplayHeight()); // Seekbar progress = 0
            binding.alpha.setAlphaPercentage(90);
            binding.gravity.setGravityCheckId(R.id.rbtn_top_start);
            binding.width.setWidthPixels(400);
            binding.width.setWidthCheckId(R.id.rbtn_width_wc);
            binding.height.setHeightPixels(400);
            binding.height.setHeightCheckId(R.id.rbtn_height_wc);
            binding.margins.setVerticalMarginPercentage(0);
            binding.margins.setHorizontalMarginPercentage(0);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        overlayView.hide();
    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.sample_text_view:
                overlayView.getView()
                        .setText("Clicked: " + SimpleDateFormat.getTimeInstance().format(new Date()));
                break;
            case R.id.btn_show:
                overlayView.getView().setText("overlayView.show()");
                overlayView.show();
                break;
            case R.id.btn_hide:
                overlayView.hide();
                break;
        }
    }

    TextView createTextView() {
        TextView textView = new TextView(this);
        textView.setId(R.id.sample_text_view);
        TextViewCompat.setTextAppearance(textView, R.style.TextAppearance_AppCompat_Medium);
        textView.setText("SAMPLE TEXT");
        textView.setTextColor(Color.WHITE);
        textView.setBackgroundColor(Color.RED);
        textView.setPadding(10, 10, 10, 10);
        return textView;
    }

    private void updateText(String str, Object... args) {
        String newText = String.format(str, args);
        overlayView.getView().setText(newText);
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Timber.d("onCheckedChanged id:%d, isChecked:%s", buttonView.getId(), isChecked);
        switch (buttonView.getId()) {
            case R.id.chk_touchable:
                binding.flags.setIsTouchable(isChecked);
                overlayView.setTouchable(isChecked).update();
                updateText("overlayView\n  .setTouchable(%s)\n  .update();", isChecked);
                break;
            case R.id.chk_draggable:
                binding.flags.setIsDraggable(isChecked);
                overlayView.setDraggable(isChecked).update();
                updateText("overlayView\n  .setDraggable(%s)\n  .update();", isChecked);
                break;
            case R.id.chk_allowViewToExtendOutsideScreen:
                binding.flags.setAllowViewToExtendOutsideScreen(isChecked);
                overlayView.allowViewToExtendOutsideScreen(isChecked).update();
                updateText("overlayView\n  .allowViewToExtendOutsideScreen(%s)\n  .update();", isChecked);
                break;
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        Timber.d("onProgressChanged seekBarId:%d, progress:%d", seekBar.getId(), progress);
        switch (seekBar.getId()) {
            case R.id.seek_alpha:
                binding.alpha.setAlphaPercentage(progress);
                float alpha = progress / 100f;
                overlayView.setAlpha(alpha).update();
                updateText("overlayView\n  .setAlpha(%.2f)\n  .update();", alpha);
                break;
            case R.id.seek_x:
                binding.xy.setXSeek(progress);
                int x = progress - binding.getDisplayWidth();
                overlayView.setX(x).update();
                updateText("overlayView\n  .setX(%d)\n  .update()", x);
                break;
            case R.id.seek_y:
                binding.xy.setYSeek(progress);
                int y = progress - binding.getDisplayHeight();
                overlayView.setY(y).update();
                updateText("overlayView\n  .setY(%d)\n  .update()", y);
                break;
            case R.id.seek_width_px:
                binding.width.setWidthPixels(progress);
                if (binding.width.getWidthCheckId() == R.id.rbtn_width_px) {
                    overlayView.setWidth(progress).update();
                    updateText("overlayView\n  .setWidth(%d)\n  .update()", progress);
                }
                break;
            case R.id.seek_height_px:
                binding.height.setHeightPixels(progress);
                if (binding.height.getHeightCheckId() == R.id.rbtn_height_px) {
                    overlayView.setHeight(progress).update();
                    updateText("overlayView\n  .setHeight(%d)\n  .update()", progress);
                }
                break;
            case R.id.seek_margin_vertical:
                binding.margins.setVerticalMarginPercentage(progress);
                float verticalMargin = progress / 100f;
                overlayView.setVerticalMargin(verticalMargin).update();
                updateText("overlayView\n  .setVerticalMargin(%.2f)\n  .update()", verticalMargin);
                break;
            case R.id.seek_margin_horizontal:
                binding.margins.setHorizontalMarginPercentage(progress);
                float horizontalMargin = progress / 100f;
                overlayView.setHorizontalMargin(horizontalMargin).update();
                updateText("overlayView\n  .setHorizontalMargin(%.2f)\n  .update()", horizontalMargin);
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        Timber.d("onCheckedChanged groupId:%d, checkedId:%d", group.getId(), checkedId);
        switch (group.getId()) {
            case R.id.rgrp_gravity:
                binding.gravity.setGravityCheckId(checkedId);
                overlayView.setGravity(getGravityValue(checkedId)).update();
                updateText("overlayView\n  .setGravity(%s)\n  .update()", getGravityValueString(checkedId));
                break;
            case R.id.rgrp_width:
                binding.width.setWidthCheckId(checkedId);
                switch (checkedId) {
                    case R.id.rbtn_width_wc:
                        overlayView.setWidth(WRAP_CONTENT).update();
                        updateText("overlayView\n  .setWidth(WRAP_CONTENT)\n  .update()");
                        break;
                    case R.id.rbtn_width_mp:
                        overlayView.setWidth(MATCH_PARENT).update();
                        updateText("overlayView\n  .setWidth(MATCH_PARENT)\n  .update()");
                        break;
                    case R.id.rbtn_width_px:
                        overlayView.setWidth(binding.width.getWidthPixels()).update();
                        binding.width.setWidthPixels(binding.width.getWidthPixels());
                        updateText("overlayView\n  .setWidth(%d)\n  .update()", binding.width.getWidthPixels());
                        break;
                }
                break;
            case R.id.rgrp_height:
                binding.height.setHeightCheckId(checkedId);
                switch (checkedId) {
                    case R.id.rbtn_height_wc:
                        overlayView.setHeight(WRAP_CONTENT).update();
                        updateText("overlayView\n  .setHeight(WRAP_CONTENT)\n  .update()");
                        break;
                    case R.id.rbtn_height_mp:
                        overlayView.setHeight(MATCH_PARENT).update();
                        updateText("overlayView\n  .setHeight(MATCH_PARENT)\n  .update()");
                        break;
                    case R.id.rbtn_height_px:
                        overlayView.setHeight(binding.height.getHeightPixels()).update();
                        binding.height.setHeightPixels(binding.height.getHeightPixels());
                        updateText("overlayView\n  .setHeight(%d)\n  .update()", binding.height.getHeightPixels());
                        break;
                }
                break;
        }
    }

    private static final String KEY_FLAGS_IS_TOUCHABLE = "flags.IsTouchable";
    private static final String KEY_FLAGS_IS_DRAGGABLE = "flags.IsDraggable";
    private static final String KEY_FLAGS_ALLOW_VIEW_TO_EXTEND_OUTSIDE_SCREEN = "flags.AllowViewToExtendOutsideScreen";
    private static final String KEY_XY_XSEEK = "xy.XSeek";
    private static final String KEY_XY_YSEEK = "xy.YSeek";
    private static final String KEY_ALPHA_ALPHA_PERCENTAGE = "alpha.AlphaPercentage";
    private static final String KEY_GRAVITY_GRAVITY_CHECK_ID = "gravity.GravityCheckId";
    private static final String KEY_WIDTH_WIDTH_CHECK_ID = "width.WidthCheckId";
    private static final String KEY_WIDTH_WIDTH_PIXELS = "width.WidthPixels";
    private static final String KEY_HEIGHT_HEIGHT_CHECK_ID = "height.HeightCheckId";
    private static final String KEY_HEIGHT_HEIGHT_PIXELS = "height.HeightPixels";
    private static final String KEY_MARGINS_VERTICAL_MARGIN_PERCENTAGE = "margins.VerticalMarginPercentage";
    private static final String KEY_MARGINS_HORIZONTAL_MARGIN_PERCENTAGE = "margins.HorizontalMarginPercentage";

    private static final String KEY_IS_VISIBLE = "isVisible";

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putBoolean(KEY_IS_VISIBLE, overlayView.isVisible());
        overlayView.hide();
        outState.putBoolean(KEY_FLAGS_IS_TOUCHABLE, binding.flags.getIsTouchable());
        outState.putBoolean(KEY_FLAGS_IS_DRAGGABLE, binding.flags.getIsDraggable());
        outState.putBoolean(KEY_FLAGS_ALLOW_VIEW_TO_EXTEND_OUTSIDE_SCREEN, binding.flags.getAllowViewToExtendOutsideScreen());
        outState.putInt(KEY_XY_XSEEK, binding.xy.getXSeek() - binding.getDisplayWidth());
        outState.putInt(KEY_XY_YSEEK, binding.xy.getYSeek() - binding.getDisplayHeight());
        outState.putInt(KEY_ALPHA_ALPHA_PERCENTAGE, binding.alpha.getAlphaPercentage());
        outState.putInt(KEY_GRAVITY_GRAVITY_CHECK_ID, binding.gravity.getGravityCheckId());
        outState.putInt(KEY_WIDTH_WIDTH_CHECK_ID, binding.width.getWidthCheckId());
        outState.putInt(KEY_WIDTH_WIDTH_PIXELS, binding.width.getWidthPixels());
        outState.putInt(KEY_HEIGHT_HEIGHT_CHECK_ID, binding.height.getHeightCheckId());
        outState.putInt(KEY_HEIGHT_HEIGHT_PIXELS, binding.height.getHeightPixels());
        outState.putInt(KEY_MARGINS_VERTICAL_MARGIN_PERCENTAGE, binding.margins.getVerticalMarginPercentage());
        outState.putInt(KEY_MARGINS_HORIZONTAL_MARGIN_PERCENTAGE, binding.margins.getHorizontalMarginPercentage());
        Timber.d("onSaveInstanceState %s", outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        Timber.d("onRestoreInstanceState %s", savedInstanceState);
        boolean isTouchable = savedInstanceState.getBoolean(KEY_FLAGS_IS_TOUCHABLE);
        boolean isDraggable = savedInstanceState.getBoolean(KEY_FLAGS_IS_DRAGGABLE);
        boolean allowViewToExtendOutsideScreen = savedInstanceState.getBoolean(KEY_FLAGS_ALLOW_VIEW_TO_EXTEND_OUTSIDE_SCREEN);
        int x = savedInstanceState.getInt(KEY_XY_XSEEK);
        int y = savedInstanceState.getInt(KEY_XY_YSEEK);
        int alphaPercentage = savedInstanceState.getInt(KEY_ALPHA_ALPHA_PERCENTAGE);
        int gravityCheckId = savedInstanceState.getInt(KEY_GRAVITY_GRAVITY_CHECK_ID);
        int widthCheckId = savedInstanceState.getInt(KEY_WIDTH_WIDTH_CHECK_ID);
        int widthPixels = savedInstanceState.getInt(KEY_WIDTH_WIDTH_PIXELS);
        int heightCheckId = savedInstanceState.getInt(KEY_HEIGHT_HEIGHT_CHECK_ID);
        int heightPixels = savedInstanceState.getInt(KEY_HEIGHT_HEIGHT_PIXELS);
        int verticalMarginPercentage = savedInstanceState.getInt(KEY_MARGINS_VERTICAL_MARGIN_PERCENTAGE);
        int horizontalMarginPercentage = savedInstanceState.getInt(KEY_MARGINS_HORIZONTAL_MARGIN_PERCENTAGE);

        binding.flags.setIsTouchable(isTouchable);
        binding.flags.setIsDraggable(isDraggable);
        binding.flags.setAllowViewToExtendOutsideScreen(allowViewToExtendOutsideScreen);
        binding.xy.setXSeek(x + binding.getDisplayWidth());
        binding.xy.setYSeek(y + binding.getDisplayHeight());
        binding.alpha.setAlphaPercentage(alphaPercentage);
        binding.gravity.setGravityCheckId(gravityCheckId);
        binding.width.setWidthCheckId(widthCheckId);
        binding.width.setWidthPixels(widthPixels);
        binding.height.setHeightCheckId(heightCheckId);
        binding.height.setHeightPixels(heightPixels);
        binding.margins.setVerticalMarginPercentage(verticalMarginPercentage);
        binding.margins.setHorizontalMarginPercentage(horizontalMarginPercentage);

        overlayView.setTouchable(isTouchable)
                .setDraggable(isDraggable)
                .allowViewToExtendOutsideScreen(allowViewToExtendOutsideScreen)
                .setX(x)
                .setY(y)
                .setAlpha(alphaPercentage / 100f)
                .setGravity(getGravityValue(gravityCheckId))
                .setWidth(widthCheckId == R.id.rbtn_width_wc ? WRAP_CONTENT
                        : widthCheckId == R.id.rbtn_width_mp ? MATCH_PARENT
                        : widthPixels)
                .setHeight(heightCheckId == R.id.rbtn_height_wc ? WRAP_CONTENT
                        : heightCheckId == R.id.rbtn_height_mp ? MATCH_PARENT
                        : heightPixels)
                .setVerticalMargin(verticalMarginPercentage / 100f)
                .setHorizontalMargin(horizontalMarginPercentage / 100f);

        if (savedInstanceState.getBoolean(KEY_IS_VISIBLE)) {
            overlayView.show();
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    int getGravityValue(int gravityCheckId) {
        switch (gravityCheckId) {
            case R.id.rbtn_top_start:
                return Gravity.TOP | Gravity.START;
            case R.id.rbtn_top_center_vertical:
                return Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            case R.id.rbtn_top_end:
                return Gravity.TOP | Gravity.END;
            case R.id.rbtn_center_start:
                return Gravity.CENTER_VERTICAL | Gravity.START;
            case R.id.rbtn_center:
                return Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
            case R.id.rbtn_center_end:
                return Gravity.CENTER_VERTICAL | Gravity.END;
            case R.id.rbtn_bottom_start:
                return Gravity.BOTTOM | Gravity.START;
            case R.id.rbtn_bottom_center_vertical:
                return Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
            case R.id.rbtn_bottom_end:
                return Gravity.BOTTOM | Gravity.END;
        }
        return Gravity.NO_GRAVITY;
    }

    String getGravityValueString(int gravityCheckId) {
        switch (gravityCheckId) {
            case R.id.rbtn_top_start:
                return "TOP | START";
            case R.id.rbtn_top_center_vertical:
                return "TOP\n    | CENTER_HORIZONTAL";
            case R.id.rbtn_top_end:
                return "TOP | END";
            case R.id.rbtn_center_start:
                return "\n    CENTER_VERTICAL\n    | START";
            case R.id.rbtn_center:
                return "CENTER";
            case R.id.rbtn_center_end:
                return "\n    CENTER_VERTICAL\n    | END";
            case R.id.rbtn_bottom_start:
                return "BOTTOM | START";
            case R.id.rbtn_bottom_center_vertical:
                return "BOTTOM\n    | CENTER_HORIZONTAL";
            case R.id.rbtn_bottom_end:
                return "BOTTOM | END";
        }
        return "NO_GRAVITY";
    }
}
