package com.nagopy.android.overlayviewmanager;

import android.app.Instrumentation;
import android.content.Context;
import android.os.Build;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.SdkSuppress;
import android.support.test.runner.AndroidJUnit4;
import android.support.test.uiautomator.UiDevice;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.JELLY_BEAN_MR2)
@RunWith(AndroidJUnit4.class)
public class OverlayViewManagerTest {

    private UiDevice uiDevice;

    @Before
    public void setup() {
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        Context targetContext = instrumentation.getTargetContext();
        OverlayViewManager.init(targetContext);

        uiDevice = UiDevice.getInstance(instrumentation);
    }

    @Test
    public void getDisplayWidth() throws Exception {
        assertThat(OverlayViewManager.getDisplayWidth(), is(uiDevice.getDisplayWidth()));
    }

    @Test
    public void getDisplayHeight() throws Exception {
        assertThat(OverlayViewManager.getDisplayHeight(), is(uiDevice.getDisplayHeight()));
    }

}