package com.nagopy.android.overlayviewmanager;

import android.Manifest;
import android.app.Application;
import android.app.Instrumentation;
import android.app.UiAutomation;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.SdkSuppress;
import androidx.test.runner.AndroidJUnit4;
import androidx.test.uiautomator.UiDevice;
import android.util.Log;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.TimeoutException;

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

    // Cannot grant SYSTEM_ALERT_WINDOW permission by "pm grant pkg" command on Marshmallow.
    // So skip the test.
    // @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    public void canDrawOverlays_granted() throws Exception {
        executeShellCommand(
                "pm grant "
                        + application.getPackageName()
                        + " "
                        + Manifest.permission.SYSTEM_ALERT_WINDOW
                , 5000);

        assertTrue(overlayViewManager.canDrawOverlays());
    }

    // Cannot grant SYSTEM_ALERT_WINDOW permission by "pm grant pkg" command on Marshmallow.
    // So skip the test.
    // @SdkSuppress(minSdkVersion = Build.VERSION_CODES.M)
    @SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
    @Test
    public void canDrawOverlays_denied() throws Exception {
        executeShellCommand(
                "pm revoke "
                        + application.getPackageName()
                        + " "
                        + Manifest.permission.SYSTEM_ALERT_WINDOW
                , 5000);
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

    private void executeShellCommand(String cmd, long timeoutInMillis) throws Exception {
        ParcelFileDescriptor pfDescriptor = uiAutomation.executeShellCommand(cmd);
        long endTimeInMillis = timeoutInMillis > 0 ? System.currentTimeMillis() + timeoutInMillis : 0;
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(
                    new InputStreamReader(new ParcelFileDescriptor.AutoCloseInputStream(pfDescriptor)));
            String line;
            while ((line = reader.readLine()) != null) {
                Log.i("canDrawOverlays_API23_deny", line);
                if (endTimeInMillis > System.currentTimeMillis()) {
                    throw new TimeoutException();
                }
            }
        } finally {
            if (reader != null) {
                reader.close();
            }
        }
    }

}