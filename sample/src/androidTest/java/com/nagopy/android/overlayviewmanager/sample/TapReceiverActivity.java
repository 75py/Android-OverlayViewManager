/*
 * Copyright 2026 75py
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

import android.app.Activity;
import android.os.Bundle;
import android.os.Process;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * Cross-UID touch target for {@link CrossUidPassThroughTest}. It is declared in the test APK's
 * manifest, so it runs with the test APK's UID rather than the sample app's. It does not hide
 * non-system overlays or filter obscured touches; it counts taps on its full-screen content and
 * shows its UID.
 */
public class TapReceiverActivity extends Activity {

    static final String TAP_AREA_DESCRIPTION = "tap-receiver";
    static final String TAPS_PREFIX = "taps=";
    static final String UID_PREFIX = "uid=";

    private int taps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView uid = new TextView(this);
        uid.setText(UID_PREFIX + Process.myUid());
        TextView count = new TextView(this);
        count.setText(TAPS_PREFIX + taps);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setContentDescription(TAP_AREA_DESCRIPTION);
        root.addView(uid);
        root.addView(count);
        root.setOnClickListener(view -> count.setText(TAPS_PREFIX + ++taps));
        setContentView(root);
    }
}
