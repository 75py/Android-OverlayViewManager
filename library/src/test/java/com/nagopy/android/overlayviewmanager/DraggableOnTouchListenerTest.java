package com.nagopy.android.overlayviewmanager;

import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import com.nagopy.android.overlayviewmanager.internal.ScreenMonitor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DraggableOnTouchListenerTest {

    DraggableOnTouchListener<View> draggableOnTouchListener;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    OverlayView<View> overlayView;

    @Mock
    WindowManager.LayoutParams params;

    @Mock
    View view;

    @Mock
    ScreenMonitor screenMonitor;

    @Mock
    MotionEvent motionEvent;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        overlayView.params = params;
        when(overlayView.getView()).thenReturn(view);
        ScreenMonitor.setInstance(screenMonitor);

        draggableOnTouchListener = new DraggableOnTouchListener<>(overlayView);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void constructor() throws Exception {
        overlayView.params.alpha = 0.5f;
        DraggableOnTouchListener<View> d = new DraggableOnTouchListener<>(overlayView);

        assertThat(d.overlayView, is(equalTo(overlayView)));
        assertThat(d.screenMonitor, is(equalTo(screenMonitor)));
        assertThat(d.backupAlpha, is(0.5f));
    }

    @Test
    public void onTouch_up() throws Exception {
        when(motionEvent.getActionMasked()).thenReturn(MotionEvent.ACTION_UP);
        draggableOnTouchListener.backupAlpha = 0.5f;
        when(overlayView.setAlpha(0.5f)).thenReturn(overlayView);

        draggableOnTouchListener.onTouch(view, motionEvent);

        verify(motionEvent, times(1)).getActionMasked();
        verify(overlayView, times(1)).setAlpha(0.5f);
        verify(overlayView, times(1)).update();
    }

    @Test
    public void onTouch_cancel() throws Exception {
        when(motionEvent.getActionMasked()).thenReturn(MotionEvent.ACTION_CANCEL);
        draggableOnTouchListener.backupAlpha = 0.5f;
        when(overlayView.setAlpha(0.5f)).thenReturn(overlayView);

        draggableOnTouchListener.onTouch(view, motionEvent);

        verify(motionEvent, times(1)).getActionMasked();
        verify(overlayView, times(1)).setAlpha(0.5f);
        verify(overlayView, times(1)).update();
    }

}