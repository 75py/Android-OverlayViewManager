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

import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.widget.TextViewCompat;
import android.view.View;
import android.widget.TextView;

import com.nagopy.android.overlayviewmanager.OverlayView;

public class Sample1Activity extends BaseSampleActiity {

    OverlayView<TextView> overlayView;
    int clickCounter = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sample1);

        TextView textView = createTextView();
        overlayView = OverlayView.create(textView)
                .setTouchable(true)
                .setDraggable(true)
                .setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        clickCounter++;
                        overlayView.getView().setText("click:" + clickCounter);
                    }
                });
    }

    public void onClick(View view) {
        if (overlayView.isVisible()) {
            overlayView.hide();
        } else {
            overlayView.show();
        }
    }

    @Override
    protected void onDestroy() {
        overlayView.hide();
        super.onDestroy();
    }

    TextView createTextView() {
        TextView textView = new TextView(this);
        textView.setId(R.id.sample_text_view);
        TextViewCompat.setTextAppearance(textView, R.style.TextAppearance_AppCompat_Large);
        textView.setText("click:" + clickCounter);
        textView.setTextColor(Color.WHITE);
        textView.setBackgroundColor(Color.RED);
        textView.setPadding(10, 10, 10, 10);
        return textView;
    }
}
