package com.nagopy.android.overlayviewmanager.internal;

import android.app.Activity;
import android.view.View;
import android.view.WindowManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class OverlayWindowManagerTest {

    OverlayWindowManager overlayWindowManager;

    @Mock
    WindowManager windowManager;

    @Mock
    View view;

    @Mock
    WindowManager.LayoutParams params;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);

        overlayWindowManager = new OverlayWindowManager();
        overlayWindowManager.windowManager = windowManager;
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getApplicationInstance() throws Exception {
        OverlayWindowManager.initApplicationInstance(windowManager);
        OverlayWindowManager instance = OverlayWindowManager.getApplicationInstance();
        assertThat(instance, is(notNullValue()));
        assertThat(instance.windowManager, is(equalTo(windowManager)));
    }

    @Test
    public void getActivityInstance() throws Exception {
        Activity activity1 = mock(Activity.class);
        WindowManager windowManager1 = mock(WindowManager.class);
        when(activity1.getWindowManager()).thenReturn(windowManager1);
        Activity activity2 = mock(Activity.class);
        WindowManager windowManager2 = mock(WindowManager.class);
        when(activity2.getWindowManager()).thenReturn(windowManager2);

        OverlayWindowManager instance1 = OverlayWindowManager.getActivityInstance(activity1);
        assertThat(instance1, is(notNullValue()));
        assertThat(instance1.windowManager, is(equalTo(windowManager1)));

        OverlayWindowManager instance2 = OverlayWindowManager.getActivityInstance(activity2);
        assertThat(instance2, is(notNullValue()));
        assertThat(instance2.windowManager, is(equalTo(windowManager2)));

        OverlayWindowManager instance1_2 = OverlayWindowManager.getActivityInstance(activity1);
        assertThat(instance1_2, is(notNullValue()));
        assertThat(instance1_2.windowManager, is(equalTo(windowManager1)));
    }

    @Test
    public void show() throws Exception {
        overlayWindowManager.show(view, params);

        verify(windowManager, times(1)).addView(view, params);
    }

    @Test
    public void show_fail() throws Exception {
        doThrow(RuntimeException.class).when(windowManager).addView(view, params);

        overlayWindowManager.show(view, params);

        verify(windowManager, times(1)).addView(view, params);
    }

    @Test
    public void update() throws Exception {
        overlayWindowManager.update(view, params);

        verify(windowManager, times(1)).updateViewLayout(view, params);
    }

    @Test
    public void update_fail() throws Exception {
        doThrow(RuntimeException.class).when(windowManager).updateViewLayout(view, params);

        overlayWindowManager.update(view, params);

        verify(windowManager, times(1)).updateViewLayout(view, params);
    }

    @Test
    public void hide() throws Exception {
        overlayWindowManager.hide(view);

        verify(windowManager, times(1)).removeViewImmediate(view);
    }

    @Test
    public void hide_fail() throws Exception {
        doThrow(RuntimeException.class).when(windowManager).removeViewImmediate(view);

        overlayWindowManager.hide(view);

        verify(windowManager, times(1)).removeViewImmediate(view);
    }

}