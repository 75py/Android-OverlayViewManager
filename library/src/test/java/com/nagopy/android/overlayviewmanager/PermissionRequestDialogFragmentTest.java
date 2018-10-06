package com.nagopy.android.overlayviewmanager;

import android.app.Dialog;
import android.os.Build;
import android.os.Bundle;
import androidx.fragment.app.FragmentActivity;
import android.view.Window;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class
        , sdk = Build.VERSION_CODES.LOLLIPOP
)
public class PermissionRequestDialogFragmentTest {

    public static class TestActivity extends FragmentActivity {
        PermissionRequestDialogFragment dialogFragment;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            dialogFragment = PermissionRequestDialogFragment.newInstance(android.R.string.unknownName);
            dialogFragment.show(getSupportFragmentManager(), "some_dialog");
        }
    }

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void newInstance() throws Exception {
        PermissionRequestDialogFragment fragment = PermissionRequestDialogFragment.newInstance(75);
        assertThat(fragment, is(notNullValue()));
        assertThat(fragment.getArguments().getInt(PermissionRequestDialogFragment.KEY_APP_NAME_ID),
                is(75));
    }

    @Test
    public void onCreateDialog() throws Exception {
        TestActivity testActivity = Robolectric.setupActivity(TestActivity.class);
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertThat(dialog, is(notNullValue()));
    }

}