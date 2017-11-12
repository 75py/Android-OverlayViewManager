package com.nagopy.android.overlayviewmanager.lint;

import com.android.tools.lint.checks.infrastructure.TestFile;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static com.android.tools.lint.checks.infrastructure.TestFiles.java;
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
                .run()
                .expectWarningCount(1)
                .expectMatches("src/foo/Example\\.java:5.+")
                .expectFixDiffs("" +
                        "Fix for src/foo/Example.java line 4: Replace with new OverlayView()\n" +
                        "            :\n" +
                        "@@ -6 +6\n" +
                        "-             .allowViewToExtendOutsideScreen(true)\n" +
                        "+            \n");
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
                .run()
                .expectWarningCount(1)
                .expectMatches("src/foo/Example\\.java:6.+")
                .expectFixDiffs("" +
                        "Fix for src/foo/Example.java line 5: Replace with new OverlayView()\n" +
                        "            :\n" +
                        "@@ -7 +7\n" +
                        "-             .allowViewToExtendOutsideScreen(b)\n" +
                        "+            \n");
    }

}