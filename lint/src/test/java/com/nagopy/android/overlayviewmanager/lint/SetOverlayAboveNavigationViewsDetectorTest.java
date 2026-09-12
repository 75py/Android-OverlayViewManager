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

    private static final TestFile TEST_FILE = java(
            "package com.nagopy.android.overlayviewmanager;\n"
                    + "public class OverlayView {\n"
                    + "  public OverlayView setTouchable(boolean b) { return this; }\n"
                    + "  public OverlayView allowViewToExtendOutsideScreen(boolean b) { return this; }\n"
                    + "  public OverlayView show() { return this; }\n"
                    + "}"
    );

    private static final TestFile TEST_FILE_WITH_NO_ARG_OVERLOAD = java(
            "package com.nagopy.android.overlayviewmanager;\n"
                    + "public class OverlayView {\n"
                    + "  public OverlayView allowViewToExtendOutsideScreen(boolean b) { return this; }\n"
                    + "  public OverlayView allowViewToExtendOutsideScreen() { return this; }\n"
                    + "  public OverlayView show() { return this; }\n"
                    + "}"
    );

    private static final TestFile UNRELATED_CLASS_WITH_SAME_NAMED_METHOD = java(
            "package foo;\n"
                    + "public class NotOverlayView {\n"
                    + "  public NotOverlayView allowViewToExtendOutsideScreen(boolean b) { return this; }\n"
                    + "}"
    );

    @Before
    public void setUp() throws Exception {
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void usingSetTouchableOnNavigationViews() throws Exception {
        lint().files(TEST_FILE,
                java("package foo;\n"
                        + "import com.nagopy.android.overlayviewmanager.OverlayView;\n"
                        + "public class Example {\n"
                        + "    public void test() {\n"
                        + "        OverlayView view = new OverlayView()\n"
                        + "            .allowViewToExtendOutsideScreen(true)\n"
                        + "            .show();\n"
                        + "    }\n"
                        + "}"
                )
        ).issues(SetOverlayAboveNavigationViewsDetector.ALLOW_VIEW_TO_EXTEND_OUTSIDE_SCREEN)
                .allowMissingSdk()
                .run()
                .expectWarningCount(1)
                .expectMatches("src/foo/Example\\.java:5: Warning: Please be careful with using allowViewToExtendOutsideScreen\\(true\\).+");
    }

    @Test
    public void usingSetTouchableOnNavigationViews_false() throws Exception {
        lint().files(TEST_FILE,
                java("package foo;\n"
                        + "import com.nagopy.android.overlayviewmanager.OverlayView;\n"
                        + "public class Example {\n"
                        + "    public void test() {\n"
                        + "        OverlayView view = new OverlayView()\n"
                        + "            .setTouchable(true)\n"
                        + "            .allowViewToExtendOutsideScreen(false)\n"
                        + "            .show();\n"
                        + "    }\n"
                        + "}"
                )
        ).issues(SetOverlayAboveNavigationViewsDetector.ALLOW_VIEW_TO_EXTEND_OUTSIDE_SCREEN)
                .allowMissingSdk()
                .run()
                .expectWarningCount(0);
    }

    @Test
    public void usingSetTouchableOnNavigationViews_variable() throws Exception {
        lint().files(TEST_FILE,
                java("package foo;\n"
                        + "import com.nagopy.android.overlayviewmanager.OverlayView;\n"
                        + "public class Example {\n"
                        + "    public void test() {\n"
                        + "        boolean b = false;\n"
                        + "        OverlayView view = new OverlayView()\n"
                        + "            .allowViewToExtendOutsideScreen(b)\n"
                        + "            .show();\n"
                        + "    }\n"
                        + "}"
                )
        ).issues(SetOverlayAboveNavigationViewsDetector.ALLOW_VIEW_TO_EXTEND_OUTSIDE_SCREEN)
                .allowMissingSdk()
                .run()
                .expectWarningCount(1)
                .expectMatches("src/foo/Example\\.java:6: Warning: Please be careful with using allowViewToExtendOutsideScreen\\(true\\).+");
    }

    @Test
    public void unrelatedClassWithSameNamedMethod_isNotFlagged() throws Exception {
        lint().files(UNRELATED_CLASS_WITH_SAME_NAMED_METHOD,
                java("package foo;\n"
                        + "public class Example {\n"
                        + "    public void test() {\n"
                        + "        new NotOverlayView().allowViewToExtendOutsideScreen(true);\n"
                        + "    }\n"
                        + "}"
                )
        ).issues(SetOverlayAboveNavigationViewsDetector.ALLOW_VIEW_TO_EXTEND_OUTSIDE_SCREEN)
                .allowMissingSdk()
                .run()
                .expectWarningCount(0);
    }

    @Test
    public void noArgOverload_doesNotThrowOrFlag() throws Exception {
        lint().files(TEST_FILE_WITH_NO_ARG_OVERLOAD,
                java("package foo;\n"
                        + "import com.nagopy.android.overlayviewmanager.OverlayView;\n"
                        + "public class Example {\n"
                        + "    public void test() {\n"
                        + "        OverlayView view = new OverlayView()\n"
                        + "            .allowViewToExtendOutsideScreen()\n"
                        + "            .show();\n"
                        + "    }\n"
                        + "}"
                )
        ).issues(SetOverlayAboveNavigationViewsDetector.ALLOW_VIEW_TO_EXTEND_OUTSIDE_SCREEN)
                .allowMissingSdk()
                .run()
                .expectWarningCount(0);
    }

    @Test
    public void kotlinCaller_isFlagged() throws Exception {
        lint().files(TEST_FILE,
                kotlin("package foo\n"
                        + "\n"
                        + "import com.nagopy.android.overlayviewmanager.OverlayView\n"
                        + "\n"
                        + "fun test() {\n"
                        + "    OverlayView()\n"
                        + "        .allowViewToExtendOutsideScreen(true)\n"
                        + "        .show()\n"
                        + "}\n"
                )
        ).issues(SetOverlayAboveNavigationViewsDetector.ALLOW_VIEW_TO_EXTEND_OUTSIDE_SCREEN)
                .allowMissingSdk()
                .run()
                .expectWarningCount(1);
    }

}
