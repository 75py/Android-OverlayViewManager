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

package com.nagopy.android.overlayviewmanager.lint;

import static com.android.tools.lint.detector.api.ApiKt.CURRENT_API;

import com.android.tools.lint.client.api.Vendor;
import com.android.tools.lint.detector.api.Issue;

import java.util.Collections;
import java.util.List;

public class IssueRegistry extends com.android.tools.lint.client.api.IssueRegistry {

    private static final Vendor VENDOR = new Vendor(
            "OverlayViewManager",
            "com.nagopy.android:overlayviewmanager-lint",
            "https://github.com/75py/Android-OverlayViewManager/issues",
            null);

    @Override
    public List<Issue> getIssues() {
        return Collections.singletonList(SetOverlayAboveNavigationViewsDetector.ALLOW_VIEW_TO_EXTEND_OUTSIDE_SCREEN);
    }

    @Override
    public int getApi() {
        return CURRENT_API;
    }

    @Override
    public Vendor getVendor() {
        return VENDOR;
    }
}
