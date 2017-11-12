package com.nagopy.android.overlayviewmanager.internal;

import android.view.View;
import android.view.WindowManager;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class OverlayWindowManagerTest {

    @Mock
    WindowManager windowManager;

    @Mock
    View view;

    @Mock
    WindowManager.LayoutParams params;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        OverlayWindowManager.init(windowManager);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void getInstance() throws Exception {
        OverlayWindowManager instance = OverlayWindowManager.getInstance();
        assertThat(instance, is(notNullValue()));
        assertThat(instance.windowManager, is(equalTo(windowManager)));
    }

    @Test
    public void show() throws Exception {
        OverlayWindowManager instance = OverlayWindowManager.getInstance();
        instance.show(view, params);
        verify(windowManager, times(1)).addView(view, params);
    }

    @Test
    public void show_fail() throws Exception {
        doThrow(RuntimeException.class).when(windowManager).addView(view, params);

        OverlayWindowManager instance = OverlayWindowManager.getInstance();
        instance.show(view, params);
        verify(windowManager, times(1)).addView(view, params);
    }

    @Test
    public void update() throws Exception {
        OverlayWindowManager instance = OverlayWindowManager.getInstance();
        instance.update(view, params);
        verify(windowManager, times(1)).updateViewLayout(view, params);
    }

    @Test
    public void update_fail() throws Exception {
        doThrow(RuntimeException.class).when(windowManager).updateViewLayout(view, params);

        OverlayWindowManager instance = OverlayWindowManager.getInstance();
        instance.update(view, params);
        verify(windowManager, times(1)).updateViewLayout(view, params);
    }

    @Test
    public void hide() throws Exception {
        OverlayWindowManager instance = OverlayWindowManager.getInstance();
        instance.hide(view);
        verify(windowManager, times(1)).removeView(view);
    }

    @Test
    public void hide_fail() throws Exception {
        doThrow(RuntimeException.class).when(windowManager).removeView(view);

        OverlayWindowManager instance = OverlayWindowManager.getInstance();
        instance.hide(view);
        verify(windowManager, times(1)).removeView(view);
    }

}