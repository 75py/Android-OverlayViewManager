package com.nagopy.android.overlayviewmanager;

import android.Manifest;
import android.app.Application;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN_MR2)
@RunWith(AndroidJUnit4.class)
public class OverlayViewManagerTest {

    OverlayViewManager overlayViewManager;

    private UiDevice uiDevice;
    private Application application;
    private UiAutomation uiAutomation;

    @Before
    public void setup() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        application = (Application) instrumentation.getTargetContext().getApplicationContext();
        OverlayViewManager.init(application);
        overlayViewManager = OverlayViewManager.getInstance();

        uiDevice = UiDevice.getInstance(instrumentation);
        uiAutomation = InstrumentationRegistry.getInstrumentation().getUiAutomation();
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @Test
    public void canDrawOverlays_API23_grant() throws Exception {
        uiAutomation.executeShellCommand("pm grant "
                + application.getPackageName()
                + " "
                + Manifest.permission.SYSTEM_ALERT_WINDOW);
        Thread.sleep(2000);
        assertTrue(overlayViewManager.canDrawOverlays());
    }

    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @Test
    public void canDrawOverlays_API23_deny() throws Exception {
        uiAutomation.executeShellCommand("pm revoke "
                + application.getPackageName()
                + " "
                + Manifest.permission.SYSTEM_ALERT_WINDOW);
        Thread.sleep(2000);
        assertFalse(overlayViewManager.canDrawOverlays());
    }

    @SdkSuppress(maxSdkVersion = Build.VERSION_CODES.LOLLIPOP_MR1)
    @Test
    public void canDrawOverlays_API22() throws Exception {
        assertTrue(overlayViewManager.canDrawOverlays());
    }

    @Test
    public void getDisplayWidth() throws Exception {
        assertThat(overlayViewManager.getDisplayWidth(), is(uiDevice.getDisplayWidth()));
    }

    @Test
    public void getDisplayHeight() throws Exception {
        assertThat(overlayViewManager.getDisplayHeight(), is(uiDevice.getDisplayHeight()));
    }

}