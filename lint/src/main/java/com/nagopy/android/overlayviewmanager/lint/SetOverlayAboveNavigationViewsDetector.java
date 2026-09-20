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
import com.android.tools.lint.detector.api.ConstantEvaluator;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.LintFix;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.android.tools.lint.detector.api.SourceCodeScanner;
import com.intellij.psi.PsiMethod;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UExpression;

import java.util.Collections;
import java.util.List;

public class SetOverlayAboveNavigationViewsDetector extends Detector implements SourceCodeScanner {

    private static final String OVERLAY_SPEC_BUILDER_CLASS = "com.nagopy.android.overlayviewmanager.OverlaySpec.Builder";

    static final Issue ALLOW_OUTSIDE_BOUNDS = Issue.create("ALLOW_OUTSIDE_BOUNDS",
            "Using OverlaySpec.Builder#setAllowOutsideBounds",
            "If you use this method and show a wrong size view, you cannot do anything.",
            Category.MESSAGES,
            5,
            Severity.WARNING,
            new Implementation(SetOverlayAboveNavigationViewsDetector.class, Scope.JAVA_FILE_SCOPE));

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList("setAllowOutsideBounds");
    }

    @Override
    public void visitMethodCall(JavaContext context, UCallExpression node, PsiMethod method) {
        if (!context.getEvaluator().isMemberInClass(method, OVERLAY_SPEC_BUILDER_CLASS)) {
            // A same-named method on an unrelated class must not be flagged.
            return;
        }

        List<UExpression> arguments = node.getValueArguments();
        if (arguments.size() != 1) {
            // Guard against unexpected call shapes so an unrelated overload never throws.
            return;
        }

        UExpression argument = arguments.get(0);
        String argumentSource = argument.asSourceString();
        // Warn unless the argument is provably the constant false; a Kotlin literal's
        // toString is not its source text, so evaluate the constant instead.
        Object constant = ConstantEvaluator.evaluate(context, argument);
        if (!Boolean.FALSE.equals(constant)) {
            LintFix.GroupBuilder fixGrouper = fix().group();
            String oldText = node.asSourceString();
            // delete
            fixGrouper.add(fix()
                    .replace()
                    .text(oldText)
                    .shortenNames()
                    .reformat(true)
                    .with(oldText.replace(".setAllowOutsideBounds(" + argumentSource + ")", ""))
                    .build()
            );

            context.report(ALLOW_OUTSIDE_BOUNDS, node, context.getLocation(node),
                    "Please be careful with using setAllowOutsideBounds(true)", fixGrouper.build());
        }
    }
}
