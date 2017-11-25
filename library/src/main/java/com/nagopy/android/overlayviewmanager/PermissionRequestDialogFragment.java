/*
 * Copyright 2017 75py
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nagopy.android.overlayviewmanager;


import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.annotation.VisibleForTesting;
import android.support.v4.app.DialogFragment;

/**
 * Permission request dialog.
 */
public class PermissionRequestDialogFragment extends DialogFragment {

    @VisibleForTesting
    static final String KEY_APP_NAME_ID = "appNameId";

    public PermissionRequestDialogFragment() {
        super();
    }

    /**
     * Create new instance of {@link PermissionRequestDialogFragment}.
     *
     * @param appNameId String resource id of application name
     * @return instance
     */
    public static PermissionRequestDialogFragment newInstance(@StringRes int appNameId) {
        PermissionRequestDialogFragment dialog = new PermissionRequestDialogFragment();
        Bundle args = new Bundle();
        args.putInt(KEY_APP_NAME_ID, appNameId);
        dialog.setArguments(args);
        return dialog;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        int appNameId = getArguments().getInt(KEY_APP_NAME_ID);
        String appName = getString(appNameId);
        String msg = getString(
                R.string.overlayviewmanager_this_app_needs_permission
                , appName);
        return new AlertDialog.Builder(getActivity())
                .setTitle(appNameId)
                .setMessage(msg)
                .setPositiveButton(R.string.overlayviewmanager_grant_permission, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        OverlayViewManager.getInstance().requestOverlayPermission();
                    }
                })
                .create();
    }
}