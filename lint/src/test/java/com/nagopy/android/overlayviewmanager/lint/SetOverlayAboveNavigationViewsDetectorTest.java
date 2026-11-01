package com.nagopy.android.overlayviewmanager.lint;

import com.android.tools.lint.checks.infrastructure.TestFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.android.tools.lint.checks.infrastructure.TestMode;

import static com.android.tools.lint.checks.infrastructure.TestFiles.java;
import static com.android.tools.lint.checks.infrastructure.TestFiles.kotlin;
import static com.android.tools.lint.checks.infrastructure.TestLintTask.lint;

public class SetOverlayAboveNavigationViewsDetectorTest {

    private static final TestFile OVERLAY_SPEC_JAVA = java(
            "package com.nagopy.android.overlayviewmanager;\n"
                    + "public class OverlaySpec {\n"
                    + "  public static class Builder {\n"
                    + "    public Builder setAllowOutsideBounds(boolean b) { return this; }\n"
                    + "    public OverlaySpec build() { return new OverlaySpec(); }\n"
                    + "  }\n"
                    + "}"
    );

    private static final TestFile OVERLAY_SPEC_KOTLIN = kotlin(
            "package com.nagopy.android.overlayviewmanager\n"
                    + "class OverlaySpec {\n"
                    + "    class Builder {\n"
                    + "        private var allowOutsideBounds: Boolean = false\n"
                    + "        fun setAllowOutsideBounds(value: Boolean): Builder = apply { allowOutsideBounds = value }\n"
                    + "        fun build(): OverlaySpec = OverlaySpec()\n"
                    + "    }\n"
                    + "}"
    );

    private static final TestFile OVERLAY_SPEC_WITH_NO_ARG_OVERLOAD = java(
            "package com.nagopy.android.overlayviewmanager;\n"
                    + "public class OverlaySpec {\n"
                    + "  public static class Builder {\n"
                    + "    public Builder setAllowOutsideBounds(boolean b) { return this; }\n"
                    + "    public Builder setAllowOutsideBounds() { return this; }\n"
                    + "    public OverlaySpec build() { return new OverlaySpec(); }\n"
                    + "  }\n"
                    + "}"
    );

    private static final TestFile UNRELATED_CLASS_WITH_SAME_NAMED_METHOD = java(
            "package foo;\n"
                    + "public class NotOverlaySpecBuilder {\n"
                    + "  public NotOverlaySpecBuilder setAllowOutsideBounds(boolean b) { return this; }\n"
                    + "}"
    );

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void allowOutsideBounds_true_isFlagged() throws Exception {
        lint().files(OVERLAY_SPEC_JAVA,
                java("package foo;\n"
                        + "import com.nagopy.android.overlayviewmanager.OverlaySpec;\n"
                        + "public class Example {\n"
                        + "    public void test() {\n"
                        + "        new OverlaySpec.Builder()\n"
                        + "            .setAllowOutsideBounds(true)\n"
                        + "            .build();\n"
                        + "    }\n"
                        + "}"
                )
        ).issues(SetOverlayAboveNavigationViewsDetector.ALLOW_OUTSIDE_BOUNDS)
                .allowMissingSdk()
                .run()
                .expectWarningCount(1)
                .expectMatches("src/foo/Example\\.java:5: Warning: Please be careful with using setAllowOutsideBounds\\(true\\).+");
    }

    @Test
    public void allowOutsideBounds_false_isNotFlagged() throws Exception {
        lint().files(OVERLAY_SPEC_JAVA,
                java("package foo;\n"
                        + "import com.nagopy.android.overlayviewmanager.OverlaySpec;\n"
                        + "public class Example {\n"
                        + "    public void test() {\n"
                        + "        new OverlaySpec.Builder()\n"
                        + "            .setAllowOutsideBounds(false)\n"
                        + "            .build();\n"
                        + "    }\n"
                        + "}"
                )
        ).issues(SetOverlayAboveNavigationViewsDetector.ALLOW_OUTSIDE_BOUNDS)
                .allowMissingSdk()
                .run()
                .expectWarningCount(0);
    }

    @Test
    public void allowOutsideBounds_variable_isFlagged() throws Exception {
        lint().files(OVERLAY_SPEC_JAVA,
                java("package foo;\n"
                        + "import com.nagopy.android.overlayviewmanager.OverlaySpec;\n"
                        + "public class Example {\n"
                        + "    public void test(boolean b) {\n"
                        + "        new OverlaySpec.Builder()\n"
                        + "            .setAllowOutsideBounds(b)\n"
                        + "            .build();\n"
                        + "    }\n"
                        + "}"
                )
        ).issues(SetOverlayAboveNavigationViewsDetector.ALLOW_OUTSIDE_BOUNDS)
                .allowMissingSdk()
                .run()
                .expectWarningCount(1)
                .expectMatches("src/foo/Example\\.java:5: Warning: Please be careful with using setAllowOutsideBounds\\(true\\).+");
    }

    @Test
    public void allowOutsideBounds_constantFalseVariable_isNotFlagged() throws Exception {
        lint().files(OVERLAY_SPEC_JAVA,
                java("package foo;\n"
                        + "import com.nagopy.android.overlayviewmanager.OverlaySpec;\n"
                        + "public class Example {\n"
                        + "    public void test() {\n"
                        + "        boolean b = false;\n"
                        + "        new OverlaySpec.Builder()\n"
                        + "            .setAllowOutsideBounds(b)\n"
                        + "            .build();\n"
                        + "    }\n"
                        + "}"
                )
        ).issues(SetOverlayAboveNavigationViewsDetector.ALLOW_OUTSIDE_BOUNDS)
                .allowMissingSdk()
                .run()
                .expectWarningCount(0);
    }

    @Test
    public void unrelatedClassWithSameNamedMethod_isNotFlagged() throws Exception {
        lint().files(UNRELATED_CLASS_WITH_SAME_NAMED_METHOD,
                java("package foo;\n"
                        + "public class Example {\n"
                        + "    public void test() {\n"
                        + "        new NotOverlaySpecBuilder().setAllowOutsideBounds(true);\n"
                        + "    }\n"
                        + "}"
                )
        ).issues(SetOverlayAboveNavigationViewsDetector.ALLOW_OUTSIDE_BOUNDS)
                .allowMissingSdk()
                .run()
                .expectWarningCount(0);
    }

    @Test
    public void noArgOverload_doesNotThrowOrFlag() throws Exception {
        lint().files(OVERLAY_SPEC_WITH_NO_ARG_OVERLOAD,
                java("package foo;\n"
                        + "import com.nagopy.android.overlayviewmanager.OverlaySpec;\n"
                        + "public class Example {\n"
                        + "    public void test() {\n"
                        + "        new OverlaySpec.Builder()\n"
                        + "            .setAllowOutsideBounds()\n"
                        + "            .build();\n"
                        + "    }\n"
                        + "}"
                )
        ).issues(SetOverlayAboveNavigationViewsDetector.ALLOW_OUTSIDE_BOUNDS)
                .allowMissingSdk()
                .run()
                .expectWarningCount(0);
    }

    @Test
    public void kotlinCaller_isFlagged() throws Exception {
        lint().files(OVERLAY_SPEC_KOTLIN,
                kotlin("package foo\n"
                        + "\n"
                        + "import com.nagopy.android.overlayviewmanager.OverlaySpec\n"
                        + "\n"
                        + "fun test() {\n"
                        + "    OverlaySpec.Builder()\n"
                        + "        .setAllowOutsideBounds(true)\n"
                        + "        .build()\n"
                        + "}\n"
                )
        ).issues(SetOverlayAboveNavigationViewsDetector.ALLOW_OUTSIDE_BOUNDS)
                .allowMissingSdk()
                .run()
                .expectWarningCount(1);
    }

    @Test
    public void kotlinCaller_false_isNotFlagged() throws Exception {
        lint().files(OVERLAY_SPEC_KOTLIN,
                kotlin("package foo\n"
                        + "\n"
                        + "import com.nagopy.android.overlayviewmanager.OverlaySpec\n"
                        + "\n"
                        + "fun test() {\n"
                        + "    OverlaySpec.Builder()\n"
                        + "        .setAllowOutsideBounds(false)\n"
                        + "        .build()\n"
                        + "}\n"
                )
        ).issues(SetOverlayAboveNavigationViewsDetector.ALLOW_OUTSIDE_BOUNDS)
                .allowMissingSdk()
                .run()
                .expectWarningCount(0);
    }

}
