package com.nagopy.android.overlayviewmanager.internal;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.view.View;
import android.view.WindowManager;

import org.junit.Test;

public class OverlayWindowManagerTest {
    @Test public void instancesRetainTheirAssignedBackend() {
        WindowManager windows = mock(WindowManager.class);
        OverlayWindowManager.initApplicationInstance(windows);
        assertNotNull(OverlayWindowManager.getApplicationInstance());
        Activity activity = mock(Activity.class);
        WindowManager activityWindows = mock(WindowManager.class);
        when(activity.getWindowManager()).thenReturn(activityWindows);
        assertSame(OverlayWindowManager.getActivityInstance(activity), OverlayWindowManager.getActivityInstance(activity));
    }

    @Test public void backendCallsAreSynchronousAndFailuresPropagate() {
        WindowManager windows = mock(WindowManager.class);
        View view = mock(View.class);
        WindowManager.LayoutParams params = mock(WindowManager.LayoutParams.class);
        OverlayWindowManager backend = new OverlayWindowManager(windows);
        backend.show(view, params);
        backend.update(view, params);
        backend.hide(view);
        verify(windows, times(1)).addView(view, params);
        verify(windows, times(1)).updateViewLayout(view, params);
        verify(windows, times(1)).removeViewImmediate(view);
        doThrow(new IllegalStateException("rejected")).when(windows).addView(view, params);
        assertThrows(IllegalStateException.class, () -> backend.show(view, params));
    }
}
