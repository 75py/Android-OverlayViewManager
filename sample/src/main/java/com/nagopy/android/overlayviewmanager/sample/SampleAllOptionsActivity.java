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

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import android.graphics.Color;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.widget.TextViewCompat;
import androidx.databinding.DataBindingUtil;

import com.nagopy.android.overlayviewmanager.OverlayResult;
import com.nagopy.android.overlayviewmanager.OverlaySpec;
import com.nagopy.android.overlayviewmanager.OverlayState;
import com.nagopy.android.overlayviewmanager.OverlayTouchMode;
import com.nagopy.android.overlayviewmanager.OverlayView;
import com.nagopy.android.overlayviewmanager.OverlayViewManager;
import com.nagopy.android.overlayviewmanager.sample.databinding.ActivitySampleAllOptionsBinding;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import timber.log.Timber;

/**
 * Activity-scoped overlay driven by every {@link OverlaySpec} option. Each control derives a new
 * immutable spec from the effective one ({@code overlayView.getSpec().toBuilder()...build()}),
 * applies it with {@link OverlayView#update(OverlaySpec)} and prints the {@link OverlayResult}.
 */
public class SampleAllOptionsActivity extends AppCompatActivity implements
        CompoundButton.OnCheckedChangeListener
        , SeekBar.OnSeekBarChangeListener
        , RadioGroup.OnCheckedChangeListener {

    ActivitySampleAllOptionsBinding binding;
    OverlayView<TextView> overlayView;
    private boolean attachedBeforeStop;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Timber.d("onCreate %s", savedInstanceState);

        TextView textView = createTextView();
        textView.setOnClickListener(v -> updateText("Clicked: %s", SimpleDateFormat.getTimeInstance().format(new Date())));
        overlayView = OverlayViewManager.getInstance().newOverlayView(
                textView,
                this,
                new OverlaySpec.Builder().setAlpha(0.9f).build());

        binding = DataBindingUtil.setContentView(this, R.layout.activity_sample_all_options);
        binding.setOnCheckedChangeListener(this);
        binding.setOnProgressChanged(this);
        binding.setOnRadioCheckedChangeListener(this);

        // The slider ranges come from this Activity's window, not from the display or from the
        // overlay permission. Multi-window and system bars make display-wide pixels ambiguous.
        Rect bounds = hostWindowBounds();
        binding.setDisplayWidth(bounds.width());
        binding.setDisplayHeight(bounds.height());

        if (savedInstanceState == null) {
            binding.xy.setXSeek(bounds.width()); // Seekbar progress = 0
            binding.xy.setYSeek(bounds.height()); // Seekbar progress = 0
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
        attachedBeforeStop = overlayView.getState() == OverlayState.ATTACHED;
        logIfFailed("hide()", overlayView.hide());
    }

    @Override
    protected void onDestroy() {
        logIfFailed("dispose()", overlayView.dispose());
        super.onDestroy();
    }

    public void onClick(View view) {
        if (view.getId() == R.id.btn_show) {
            OverlayResult result = overlayView.show();
            updateText("overlayView.show()\n// %s", describe(result));
            logIfFailed("show()", result);
        } else if (view.getId() == R.id.btn_hide) {
            logIfFailed("hide()", overlayView.hide());
        }
    }

    private Rect hostWindowBounds() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return getWindowManager().getCurrentWindowMetrics().getBounds();
        }
        DisplayMetrics metrics = getResources().getDisplayMetrics();
        return new Rect(0, 0, metrics.widthPixels, metrics.heightPixels);
    }

    TextView createTextView() {
        TextView textView = new TextView(this);
        textView.setId(R.id.sample_text_view);
        TextViewCompat.setTextAppearance(textView, androidx.appcompat.R.style.TextAppearance_AppCompat_Medium);
        textView.setText("SAMPLE TEXT");
        textView.setTextColor(Color.WHITE);
        textView.setBackgroundColor(Color.RED);
        textView.setPadding(10, 10, 10, 10);
        return textView;
    }

    private void updateText(String str, Object... args) {
        overlayView.getView().setText(String.format(Locale.US, str, args));
    }

    /**
     * Applies {@code next} with {@link OverlayView#update(OverlaySpec)} and shows both the code that
     * produced it and the result on the overlay itself. {@code code} is the builder call chain for
     * the changed field, e.g. {@code .setAlpha(0.90f)}.
     */
    private void applySpec(OverlaySpec next, String code) {
        OverlayResult result = overlayView.update(next);
        updateText("overlayView.update(\n  overlayView.getSpec().toBuilder()\n    %s\n    .build());\n// %s", code, describe(result));
        logIfFailed("update()", result);
    }

    private OverlaySpec.Builder spec() {
        return overlayView.getSpec().toBuilder();
    }

    private static String describe(OverlayResult result) {
        if (result.isSuccess()) {
            return result.getState() + (result.getChanged() ? "" : " (no change)");
        }
        return "FAILED " + result.getFailure() + " (state " + result.getState() + ")";
    }

    private static void logIfFailed(String action, OverlayResult result) {
        if (!result.isSuccess()) {
            Timber.w(result.getCause(), "%s failed: %s", action, result.getFailure());
        }
    }

    private static OverlayTouchMode touchMode(boolean touchable, boolean draggable) {
        if (draggable) {
            return OverlayTouchMode.DRAGGABLE;
        }
        return touchable ? OverlayTouchMode.INTERACTIVE : OverlayTouchMode.PASS_THROUGH;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Timber.d("onCheckedChanged id:%d, isChecked:%s", buttonView.getId(), isChecked);
        if (buttonView.getId() == R.id.chk_touchable) {
            binding.flags.setIsTouchable(isChecked);
            OverlayTouchMode mode = touchMode(isChecked, binding.flags.getIsDraggable());
            applySpec(spec().setTouchMode(mode).build(), ".setTouchMode(OverlayTouchMode." + mode + ")");
        } else if (buttonView.getId() == R.id.chk_draggable) {
            binding.flags.setIsDraggable(isChecked);
            OverlayTouchMode mode = touchMode(binding.flags.getIsTouchable(), isChecked);
            applySpec(spec().setTouchMode(mode).build(), ".setTouchMode(OverlayTouchMode." + mode + ")");
        } else if (buttonView.getId() == R.id.chk_allowViewToExtendOutsideScreen) {
            binding.flags.setAllowViewToExtendOutsideScreen(isChecked);
            // allowOutsideBounds lets the window extend past the parent bounds; it does not
            // guarantee anything about drawing over system bars.
            applySpec(spec().setAllowOutsideBounds(isChecked).build(), ".setAllowOutsideBounds(" + isChecked + ")");
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        Timber.d("onProgressChanged seekBarId:%d, progress:%d", seekBar.getId(), progress);
        if (seekBar.getId() == R.id.seek_alpha) {
            binding.alpha.setAlphaPercentage(progress);
            float alpha = progress / 100f;
            applySpec(spec().setAlpha(alpha).build(), String.format(Locale.US, ".setAlpha(%.2ff)", alpha));
        } else if (seekBar.getId() == R.id.seek_x) {
            binding.xy.setXSeek(progress);
            int x = progress - binding.getDisplayWidth();
            applySpec(spec().setX(x).build(), ".setX(" + x + ")");
        } else if (seekBar.getId() == R.id.seek_y) {
            binding.xy.setYSeek(progress);
            int y = progress - binding.getDisplayHeight();
            applySpec(spec().setY(y).build(), ".setY(" + y + ")");
        } else if (seekBar.getId() == R.id.seek_width_px) {
            binding.width.setWidthPixels(progress);
            if (binding.width.getWidthCheckId() == R.id.rbtn_width_px) {
                applySpec(spec().setWidth(progress).build(), ".setWidth(" + progress + ")");
            }
        } else if (seekBar.getId() == R.id.seek_height_px) {
            binding.height.setHeightPixels(progress);
            if (binding.height.getHeightCheckId() == R.id.rbtn_height_px) {
                applySpec(spec().setHeight(progress).build(), ".setHeight(" + progress + ")");
            }
        } else if (seekBar.getId() == R.id.seek_margin_vertical) {
            binding.margins.setVerticalMarginPercentage(progress);
            float verticalMargin = progress / 100f;
            applySpec(spec().setVerticalMargin(verticalMargin).build(), String.format(Locale.US, ".setVerticalMargin(%.2ff)", verticalMargin));
        } else if (seekBar.getId() == R.id.seek_margin_horizontal) {
            binding.margins.setHorizontalMarginPercentage(progress);
            float horizontalMargin = progress / 100f;
            applySpec(spec().setHorizontalMargin(horizontalMargin).build(), String.format(Locale.US, ".setHorizontalMargin(%.2ff)", horizontalMargin));
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        Timber.d("onCheckedChanged groupId:%d, checkedId:%d", group.getId(), checkedId);
        if (group.getId() == R.id.rgrp_gravity) {
            binding.gravity.setGravityCheckId(checkedId);
            applySpec(spec().setGravity(getGravityValue(checkedId)).build(), ".setGravity(Gravity." + getGravityValueString(checkedId) + ")");
        } else if (group.getId() == R.id.rgrp_width) {
            binding.width.setWidthCheckId(checkedId);
            if (checkedId == R.id.rbtn_width_wc) {
                applySpec(spec().setWidth(WRAP_CONTENT).build(), ".setWidth(WRAP_CONTENT)");
            } else if (checkedId == R.id.rbtn_width_mp) {
                applySpec(spec().setWidth(MATCH_PARENT).build(), ".setWidth(MATCH_PARENT)");
            } else if (checkedId == R.id.rbtn_width_px) {
                int width = binding.width.getWidthPixels();
                applySpec(spec().setWidth(width).build(), ".setWidth(" + width + ")");
            }
        } else if (group.getId() == R.id.rgrp_height) {
            binding.height.setHeightCheckId(checkedId);
            if (checkedId == R.id.rbtn_height_wc) {
                applySpec(spec().setHeight(WRAP_CONTENT).build(), ".setHeight(WRAP_CONTENT)");
            } else if (checkedId == R.id.rbtn_height_mp) {
                applySpec(spec().setHeight(MATCH_PARENT).build(), ".setHeight(MATCH_PARENT)");
            } else if (checkedId == R.id.rbtn_height_px) {
                int height = binding.height.getHeightPixels();
                applySpec(spec().setHeight(height).build(), ".setHeight(" + height + ")");
            }
        }
    }

    private static final String KEY_FLAGS_IS_TOUCHABLE = "flags.IsTouchable";
    private static final String KEY_FLAGS_IS_DRAGGABLE = "flags.IsDraggable";
    private static final String KEY_FLAGS_ALLOW_OUTSIDE_BOUNDS = "flags.AllowOutsideBounds";
    private static final String KEY_XY_X = "xy.X";
    private static final String KEY_XY_Y = "xy.Y";
    private static final String KEY_ALPHA_ALPHA_PERCENTAGE = "alpha.AlphaPercentage";
    private static final String KEY_GRAVITY_GRAVITY_CHECK_ID = "gravity.GravityCheckId";
    private static final String KEY_WIDTH_WIDTH_CHECK_ID = "width.WidthCheckId";
    private static final String KEY_WIDTH_WIDTH_PIXELS = "width.WidthPixels";
    private static final String KEY_HEIGHT_HEIGHT_CHECK_ID = "height.HeightCheckId";
    private static final String KEY_HEIGHT_HEIGHT_PIXELS = "height.HeightPixels";
    private static final String KEY_MARGINS_VERTICAL_MARGIN_PERCENTAGE = "margins.VerticalMarginPercentage";
    private static final String KEY_MARGINS_HORIZONTAL_MARGIN_PERCENTAGE = "margins.HorizontalMarginPercentage";
    private static final String KEY_IS_ATTACHED = "isAttached";

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // On API 28+ this runs after onStop(), which already hid the overlay; remember what the
        // user saw before the stop instead of the current state.
        outState.putBoolean(KEY_IS_ATTACHED, attachedBeforeStop || overlayView.getState() == OverlayState.ATTACHED);
        outState.putBoolean(KEY_FLAGS_IS_TOUCHABLE, binding.flags.getIsTouchable());
        outState.putBoolean(KEY_FLAGS_IS_DRAGGABLE, binding.flags.getIsDraggable());
        outState.putBoolean(KEY_FLAGS_ALLOW_OUTSIDE_BOUNDS, binding.flags.getAllowViewToExtendOutsideScreen());
        outState.putInt(KEY_XY_X, binding.xy.getXSeek() - binding.getDisplayWidth());
        outState.putInt(KEY_XY_Y, binding.xy.getYSeek() - binding.getDisplayHeight());
        outState.putInt(KEY_ALPHA_ALPHA_PERCENTAGE, binding.alpha.getAlphaPercentage());
        outState.putInt(KEY_GRAVITY_GRAVITY_CHECK_ID, binding.gravity.getGravityCheckId());
        outState.putInt(KEY_WIDTH_WIDTH_CHECK_ID, binding.width.getWidthCheckId());
        outState.putInt(KEY_WIDTH_WIDTH_PIXELS, binding.width.getWidthPixels());
        outState.putInt(KEY_HEIGHT_HEIGHT_CHECK_ID, binding.height.getHeightCheckId());
        outState.putInt(KEY_HEIGHT_HEIGHT_PIXELS, binding.height.getHeightPixels());
        outState.putInt(KEY_MARGINS_VERTICAL_MARGIN_PERCENTAGE, binding.margins.getVerticalMarginPercentage());
        outState.putInt(KEY_MARGINS_HORIZONTAL_MARGIN_PERCENTAGE, binding.margins.getHorizontalMarginPercentage());
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        boolean isTouchable = savedInstanceState.getBoolean(KEY_FLAGS_IS_TOUCHABLE);
        boolean isDraggable = savedInstanceState.getBoolean(KEY_FLAGS_IS_DRAGGABLE);
        boolean allowOutsideBounds = savedInstanceState.getBoolean(KEY_FLAGS_ALLOW_OUTSIDE_BOUNDS);
        int x = savedInstanceState.getInt(KEY_XY_X);
        int y = savedInstanceState.getInt(KEY_XY_Y);
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
        binding.flags.setAllowViewToExtendOutsideScreen(allowOutsideBounds);
        // The window may have changed size across the recreation; clamp to the new range.
        binding.xy.setXSeek(Math.max(0, Math.min(x + binding.getDisplayWidth(), binding.getDisplayWidth() * 2)));
        binding.xy.setYSeek(Math.max(0, Math.min(y + binding.getDisplayHeight(), binding.getDisplayHeight() * 2)));
        binding.alpha.setAlphaPercentage(alphaPercentage);
        binding.gravity.setGravityCheckId(gravityCheckId);
        binding.width.setWidthCheckId(widthCheckId);
        binding.width.setWidthPixels(widthPixels);
        binding.height.setHeightCheckId(heightCheckId);
        binding.height.setHeightPixels(heightPixels);
        binding.margins.setVerticalMarginPercentage(verticalMarginPercentage);
        binding.margins.setHorizontalMarginPercentage(horizontalMarginPercentage);

        OverlaySpec restored = spec()
                .setTouchMode(touchMode(isTouchable, isDraggable))
                .setAllowOutsideBounds(allowOutsideBounds)
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
                .setHorizontalMargin(horizontalMarginPercentage / 100f)
                .build();
        logIfFailed("update()", overlayView.update(restored));

        // Recreation needs a new handle and a new show(); the library never re-attaches implicitly.
        if (savedInstanceState.getBoolean(KEY_IS_ATTACHED)) {
            logIfFailed("show()", overlayView.show());
        }
        super.onRestoreInstanceState(savedInstanceState);
    }

    int getGravityValue(int gravityCheckId) {
        if (gravityCheckId == R.id.rbtn_top_start) {
            return Gravity.TOP | Gravity.START;
        } else if (gravityCheckId == R.id.rbtn_top_center_vertical) {
            return Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        } else if (gravityCheckId == R.id.rbtn_top_end) {
            return Gravity.TOP | Gravity.END;
        } else if (gravityCheckId == R.id.rbtn_center_start) {
            return Gravity.CENTER_VERTICAL | Gravity.START;
        } else if (gravityCheckId == R.id.rbtn_center) {
            return Gravity.CENTER_VERTICAL | Gravity.CENTER_HORIZONTAL;
        } else if (gravityCheckId == R.id.rbtn_center_end) {
            return Gravity.CENTER_VERTICAL | Gravity.END;
        } else if (gravityCheckId == R.id.rbtn_bottom_start) {
            return Gravity.BOTTOM | Gravity.START;
        } else if (gravityCheckId == R.id.rbtn_bottom_center_vertical) {
            return Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        } else if (gravityCheckId == R.id.rbtn_bottom_end) {
            return Gravity.BOTTOM | Gravity.END;
        }
        return Gravity.NO_GRAVITY;
    }

    String getGravityValueString(int gravityCheckId) {
        if (gravityCheckId == R.id.rbtn_top_start) {
            return "TOP | Gravity.START";
        } else if (gravityCheckId == R.id.rbtn_top_center_vertical) {
            return "TOP | Gravity.CENTER_HORIZONTAL";
        } else if (gravityCheckId == R.id.rbtn_top_end) {
            return "TOP | Gravity.END";
        } else if (gravityCheckId == R.id.rbtn_center_start) {
            return "CENTER_VERTICAL | Gravity.START";
        } else if (gravityCheckId == R.id.rbtn_center) {
            return "CENTER";
        } else if (gravityCheckId == R.id.rbtn_center_end) {
            return "CENTER_VERTICAL | Gravity.END";
        } else if (gravityCheckId == R.id.rbtn_bottom_start) {
            return "BOTTOM | Gravity.START";
        } else if (gravityCheckId == R.id.rbtn_bottom_center_vertical) {
            return "BOTTOM | Gravity.CENTER_HORIZONTAL";
        } else if (gravityCheckId == R.id.rbtn_bottom_end) {
            return "BOTTOM | Gravity.END";
        }
        return "NO_GRAVITY";
    }
}
