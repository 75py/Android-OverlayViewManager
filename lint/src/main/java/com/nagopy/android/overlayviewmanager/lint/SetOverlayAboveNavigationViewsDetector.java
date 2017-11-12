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

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiMethod;

import org.jetbrains.uast.UCallExpression;

import java.util.Collections;
import java.util.List;

public class SetOverlayAboveNavigationViewsDetector extends Detector implements Detector.UastScanner {

    static final Issue ALLOW_VIEW_TO_EXTEND_OUTSIDE_SCREEN = Issue.create("ALLOW_VIEW_TO_EXTEND_OUTSIDE_SCREEN",
            "Using OverlayView#allowViewToExtendOutsideScreen",
            "If you use this method and show a wrong size view, you cannot do anything.",
            Category.MESSAGES,
            5,
            Severity.WARNING,
            new Implementation(SetOverlayAboveNavigationViewsDetector.class, Scope.JAVA_FILE_SCOPE));

    @Override
    public List<String> getApplicableCallNames() {
        return Collections.singletonList("com.nagopy.android.overlayviewmanager.OverlayView");
    }

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList("allowViewToExtendOutsideScreen");
    }

    @Override
    public void visitMethod(JavaContext context, UCallExpression node, PsiMethod method) {
        Object arg = node.getValueArguments().get(0);
        String argStr = arg == null ? "" : arg.toString();
        if (arg != null && !argStr.equals("false")) {
            LintFix.GroupBuilder fixGrouper = fix().group();
            String oldText = node.asSourceString();
            // delete
            fixGrouper.add(fix()
                    .replace()
                    .text(oldText)
                    .shortenNames()
                    .reformat(true)
                    .with(oldText.replace(".allowViewToExtendOutsideScreen(" + argStr + ")", ""))
                    .build()
            );

            context.report(ALLOW_VIEW_TO_EXTEND_OUTSIDE_SCREEN, node, context.getLocation(node),
                    "Please be careful with using allowViewToExtendOutsideScreen(true)", fixGrouper.build());
        }
    }
}
