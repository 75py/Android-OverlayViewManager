package com.nagopy.android.overlayviewmanager;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import android.app.Activity;
import android.app.Application;
import android.os.Build;
import android.view.View;
import android.widget.FrameLayout;
import android.view.WindowManager;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = Build.VERSION_CODES.P, manifest = Config.NONE)
public class OverlayViewManagerTest {
    @After public void tearDown() { OverlayViewManager.resetForTesting(); }
    @Test public void getInstanceBeforeInit_throws() { assertThrows(IllegalStateException.class, OverlayViewManager::getInstance); }
    @Test public void sameApplicationInitIsIdempotentAndDifferentApplicationFails() {
        Application application = RuntimeEnvironment.getApplication();
        OverlayViewManager.init(application);
        OverlayViewManager.init(application);
        assertThrows(IllegalStateException.class, () -> OverlayViewManager.init(Mockito.mock(Application.class)));
    }
    @Test public void explicitJavaFactoryOverloadsCreateBothScopes() {
        Application application = RuntimeEnvironment.getApplication();
        OverlayViewManager.init(application);
        Activity activity = Mockito.mock(Activity.class);
        Mockito.when(activity.getWindowManager()).thenReturn(Mockito.mock(WindowManager.class));
        OverlayViewManager manager = OverlayViewManager.getInstance();
        assertNotNull(manager.newOverlayView(new View(application)));
        assertNotNull(manager.newOverlayView(new View(application), new OverlaySpec()));
        assertNotNull(manager.newOverlayView(new View(application), activity));
        assertNotNull(manager.newOverlayView(new View(application), activity, new OverlaySpec()));

        OverlayView<View> handle = manager.newOverlayView(new View(application), activity);
        OverlayResult configured = handle.update(handle.getSpec().toBuilder().setX(4).build());
        OverlayResult shown = handle.show();
        OverlayResult hidden = handle.hide();
        OverlayResult disposed = handle.dispose();
        assertNotNull(configured);
        assertNotNull(shown);
        assertNotNull(hidden);
        assertNotNull(disposed);
    }
    @Test public void applicationBrightnessIsRejectedAtFactory() {
        Application application = RuntimeEnvironment.getApplication();
        OverlayViewManager.init(application);
        OverlaySpec spec = new OverlaySpec.Builder().setScreenBrightness(.5f).build();
        assertThrows(IllegalArgumentException.class, () -> OverlayViewManager.getInstance().newOverlayView(new View(application), spec));
    }
    @Test public void factoryRejectsAnAlreadyParentedViewAndDestroyedActivity() {
        Application application = RuntimeEnvironment.getApplication();
        OverlayViewManager.init(application);
        View child = new View(application);
        new FrameLayout(application).addView(child);
        assertThrows(IllegalArgumentException.class, () -> OverlayViewManager.getInstance().newOverlayView(child));
        Activity destroyed = Mockito.mock(Activity.class);
        Mockito.when(destroyed.isDestroyed()).thenReturn(true);
        assertThrows(IllegalArgumentException.class, () -> OverlayViewManager.getInstance().newOverlayView(new View(application), destroyed));
    }
}
