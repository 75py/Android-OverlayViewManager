package com.nagopy.android.overlayviewmanager;

import android.os.Build;
import android.view.Gravity;
import android.view.View;

import com.nagopy.android.overlayviewmanager.internal.OverlayWindowManager;
import com.nagopy.android.overlayviewmanager.internal.ScreenMonitor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
import static android.view.WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
import static android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
import static android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL;
import static android.view.WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
import static android.view.WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class
        , sdk = Build.VERSION_CODES.LOLLIPOP
        , manifest = Config.NONE
)
public class OverlayViewTest {

    OverlayView<View> overlayView;

    @Mock
    OverlayWindowManager overlayWindowManager;

    @Mock
    ScreenMonitor screenMonitor;

    @Mock
    View view;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        OverlayWindowManager.setApplicationInstance(overlayWindowManager); // set mock
        ScreenMonitor.setInstance(screenMonitor); // set mock
        overlayView = OverlayView.create(view);
    }

    @After
    public void tearDown() throws Exception {
    }

    @Test
    public void show_isVisibleTrue() throws Exception {
        overlayView.isVisible = true;
        overlayView.show();
        verify(overlayWindowManager, never()).show(overlayView.view, overlayView.params);
        assertThat(overlayView.isVisible, is(true));
    }

    @Test
    public void show_isVisibleFalse_isDraggableFalse() throws Exception {
        overlayView.isVisible = false;
        overlayView.isDraggable = false;
        OverlayView<View> retOverlayView = overlayView.show();
        assertThat(retOverlayView, is(equalTo(overlayView)));
        verify(overlayWindowManager, times(1)).show(overlayView.view, overlayView.params);
        assertThat(overlayView.isVisible, is(true));
        assertThat(overlayView.lastWindowType, is(equalTo(overlayView.params.type)));
        verify(screenMonitor, never()).request(overlayView);
    }

    @Test
    public void show_isVisibleFalse_isDraggableTrue() throws Exception {
        overlayView.isVisible = false;
        overlayView.isDraggable = true;
        OverlayView<View> retOverlayView = overlayView.show();
        assertThat(retOverlayView, is(equalTo(overlayView)));
        verify(overlayWindowManager, times(1)).show(overlayView.view, overlayView.params);
        assertThat(overlayView.isVisible, is(true));
        assertThat(overlayView.lastWindowType, is(equalTo(overlayView.params.type)));
        verify(screenMonitor, times(1)).request(overlayView);
    }

    @Test
    public void update_isVisibleFalse() throws Exception {
        overlayView.isVisible = false;
        overlayView.update();
        verify(overlayWindowManager, never()).update(overlayView.view, overlayView.params);
        verify(screenMonitor, never()).request(overlayView);
        verify(screenMonitor, never()).cancel(overlayView);
    }

    @Test
    public void update_isVisibleTrue_sameWindowType() throws Exception {
        overlayView.lastWindowType = TYPE_SYSTEM_OVERLAY;
        overlayView.isVisible = true;
        overlayView.update();
        verify(overlayWindowManager, times(1)).update(overlayView.view, overlayView.params);
        verify(overlayWindowManager, never()).hide(overlayView.view);
        verify(overlayWindowManager, never()).show(overlayView.view, overlayView.params);
        assertThat(overlayView.lastWindowType, is(equalTo(overlayView.params.type)));
    }

    @Test
    public void update_isVisibleTrue_differentWindowType() throws Exception {
        overlayView.lastWindowType = TYPE_SYSTEM_ALERT;
        overlayView.isVisible = true;
        overlayView.update();
        verify(overlayWindowManager, never()).update(overlayView.view, overlayView.params);
        verify(overlayWindowManager, times(1)).hide(overlayView.view);
        verify(overlayWindowManager, times(1)).show(overlayView.view, overlayView.params);
        assertThat(overlayView.lastWindowType, is(equalTo(overlayView.params.type)));
    }

    @Test
    public void update_isVisibleTrue_isDraggableTrue() throws Exception {
        overlayView.isVisible = true;
        overlayView.isDraggable = true;
        overlayView.update();
        verify(screenMonitor, times(1)).request(overlayView);
        verify(screenMonitor, never()).cancel(overlayView);
    }

    @Test
    public void update_isVisibleTrue_isDraggableFalse() throws Exception {
        overlayView.isVisible = true;
        overlayView.isDraggable = false;
        overlayView.update();
        verify(screenMonitor, never()).request(overlayView);
        verify(screenMonitor, times(1)).cancel(overlayView);
    }

    @Test
    public void hide_isVisibleTrue_isDraggableTrue() throws Exception {
        overlayView.isVisible = true;
        overlayView.isDraggable = true;
        overlayView.hide();
        verify(overlayWindowManager, times(1)).hide(overlayView.view);
        verify(screenMonitor, times(1)).cancel(overlayView);
    }

    @Test
    public void hide_isVisibleTrue_isDraggableFalse() throws Exception {
        overlayView.isVisible = true;
        overlayView.isDraggable = false;
        overlayView.hide();
        verify(overlayWindowManager, times(1)).hide(overlayView.view);
        verify(screenMonitor, never()).cancel(overlayView);
    }

    @Test
    public void hide_isVisibleFalse() throws Exception {
        overlayView.isVisible = false;
        overlayView.hide();
        verify(overlayWindowManager, never()).hide(overlayView.view);
    }

    @Test
    public void getView() throws Exception {
        assertThat(overlayView.getView(), is(view));
    }

    @Test
    public void setOnClickListener() throws Exception {
        View.OnClickListener l = mock(View.OnClickListener.class);
        overlayView.setOnClickListener(l);
        verify(view, times(1)).setOnClickListener(l);
    }

    @Test
    public void setTouchable() throws Exception {
        overlayView.isTouchable = false;
        overlayView.setTouchable(true);
        assertTrue(overlayView.isTouchable);
    }

    @Test
    public void setDraggable() throws Exception {
        assertFalse(overlayView.isDraggable);

        overlayView.setDraggable(true);
        assertTrue(overlayView.isDraggable);
    }

    @Test
    public void setWidth() throws Exception {
        assertThat(overlayView.params.width, is(WRAP_CONTENT));

        overlayView.setWidth(100);
        assertThat(overlayView.params.width, is(100));
    }

    @Test
    public void setHeight() throws Exception {
        assertThat(overlayView.params.height, is(WRAP_CONTENT));

        overlayView.setHeight(200);
        assertThat(overlayView.params.height, is(200));
    }

    @Test
    public void setGravity() throws Exception {
        assertThat(overlayView.params.gravity, is(not(Gravity.BOTTOM)));

        overlayView.setGravity(Gravity.BOTTOM);
        assertThat(overlayView.params.gravity, is(Gravity.BOTTOM));
    }

    @Test
    public void setHorizontalMargin() throws Exception {
        assertThat(overlayView.params.horizontalMargin, is(0f));

        overlayView.setHorizontalMargin(0.5f);
        assertThat(overlayView.params.horizontalMargin, is(0.5f));
    }

    @Test
    public void setVerticalMargin() throws Exception {
        assertThat(overlayView.params.verticalMargin, is(0f));

        overlayView.setVerticalMargin(0.5f);
        assertThat(overlayView.params.verticalMargin, is(0.5f));
    }

    @Test
    public void setAlpha() throws Exception {
        assertThat(overlayView.params.alpha, is(1f));

        overlayView.setAlpha(0.5f);
        assertThat(overlayView.params.alpha, is(0.5f));
    }

    @Test
    public void setX() throws Exception {
        assertThat(overlayView.params.x, is(0));

        overlayView.setX(100);
        assertThat(overlayView.params.x, is(100));
    }

    @Test
    public void setY() throws Exception {
        assertThat(overlayView.params.y, is(0));

        overlayView.setY(200);
        assertThat(overlayView.params.y, is(200));
    }

    @Test
    public void getX() throws Exception {
        assertThat(overlayView.params.x, is(0));

        overlayView.params.x = 100;
        assertThat(overlayView.getX(), is(100));
    }

    @Test
    public void getY() throws Exception {
        assertThat(overlayView.params.y, is(0));

        overlayView.params.y = 200;
        assertThat(overlayView.getY(), is(200));
    }

    @Test
    public void setScreenBrightness() throws Exception {
        assertThat(overlayView.params.screenBrightness, is(BRIGHTNESS_OVERRIDE_NONE));

        overlayView.setScreenBrightness(0.5f);
        assertThat(overlayView.params.screenBrightness, is(0.5f));
    }

    @Config(sdk = Build.VERSION_CODES.N_MR1)
    @Test
    public void updateParams_API25_default() throws Exception {
        overlayView.isTouchable = false;
        overlayView.allowViewToExtendOutsideScreen = false;
        overlayView.isDraggable = false;

        overlayView.updateParams();
        assertThat(overlayView.params.type, is(TYPE_SYSTEM_OVERLAY));
        assertThat(overlayView.params.flags,
                is(equalTo(FLAG_NOT_TOUCHABLE | FLAG_NOT_TOUCH_MODAL | FLAG_NOT_FOCUSABLE)));
    }

    @Config(sdk = Build.VERSION_CODES.O)
    @Test
    public void updateParams_API26_default() throws Exception {
        overlayView.isTouchable = false;
        overlayView.allowViewToExtendOutsideScreen = false;
        overlayView.isDraggable = false;

        overlayView.updateParams();
        assertThat(overlayView.params.type, is(TYPE_APPLICATION_OVERLAY));
        assertThat(overlayView.params.flags,
                is(equalTo(FLAG_NOT_TOUCHABLE | FLAG_NOT_TOUCH_MODAL | FLAG_NOT_FOCUSABLE)));
    }

    @Config(sdk = Build.VERSION_CODES.N_MR1)
    @Test
    public void updateParams_API25_touchable() throws Exception {
        overlayView.isTouchable = true;
        overlayView.allowViewToExtendOutsideScreen = false;
        overlayView.isDraggable = false;

        overlayView.updateParams();
        assertThat(overlayView.params.type, is(TYPE_SYSTEM_ALERT));
        assertThat(overlayView.params.flags,
                is(equalTo(FLAG_NOT_FOCUSABLE)));
    }

    @Config(sdk = Build.VERSION_CODES.O)
    @Test
    public void updateParams_API26_touchable() throws Exception {
        overlayView.isTouchable = true;
        overlayView.allowViewToExtendOutsideScreen = false;
        overlayView.isDraggable = false;

        overlayView.updateParams();
        assertThat(overlayView.params.type, is(TYPE_APPLICATION_OVERLAY));
        assertThat(overlayView.params.flags,
                is(equalTo(FLAG_NOT_FOCUSABLE)));
    }

    @Config(sdk = Build.VERSION_CODES.N_MR1)
    @Test
    public void updateParams_API25_touchable_aboveNavi() throws Exception {
        overlayView.isTouchable = true;
        overlayView.allowViewToExtendOutsideScreen = true;
        overlayView.isDraggable = false;

        overlayView.updateParams();
        assertThat(overlayView.params.type, is(TYPE_SYSTEM_ERROR));
        assertThat(overlayView.params.flags,
                is(equalTo(FLAG_NOT_FOCUSABLE
                        | FLAG_LAYOUT_NO_LIMITS | FLAG_LAYOUT_INSET_DECOR | FLAG_LAYOUT_IN_SCREEN)));
    }

    @Config(sdk = Build.VERSION_CODES.O)
    @Test
    public void updateParams_API26_touchable_aboveNavi() throws Exception {
        overlayView.isTouchable = true;
        overlayView.allowViewToExtendOutsideScreen = true;
        overlayView.isDraggable = false;

        overlayView.updateParams();
        assertThat(overlayView.params.type, is(TYPE_APPLICATION_OVERLAY));
        assertThat(overlayView.params.flags,
                is(equalTo(FLAG_NOT_FOCUSABLE
                        | FLAG_LAYOUT_NO_LIMITS | FLAG_LAYOUT_INSET_DECOR | FLAG_LAYOUT_IN_SCREEN)));
    }

}