package com.aefyr.sai.ui.dialogs;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.aefyr.sai.R;

import java.util.Objects;

public class AppInstalledDialogFragment extends DialogFragment {
    private static final String ARG_PACKAGE = "package";

    private String mPackage;

    public static AppInstalledDialogFragment newInstance(String pkg) {
        AppInstalledDialogFragment fragment = new AppInstalledDialogFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PACKAGE, pkg);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args == null)
            return;

        mPackage = args.getString(ARG_PACKAGE, "");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        String appLabel = null;
        Intent appLaunchIntent = null;

        try {
            PackageManager pm = getContext().getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(mPackage, 0);
            appLabel = pm.getApplicationLabel(appInfo).toString();
            appLaunchIntent = pm.getLaunchIntentForPackage(mPackage);
            Objects.requireNonNull(appLaunchIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } catch (Exception e) {
            Log.w("SAI", e);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext())
                .setTitle(R.string.app_name)
                .setMessage(appLabel == null ? getString(R.string.installer_app_installed) : String.format(getString(R.string.installer_app_installed_full), appLabel))
                .setNegativeButton(R.string.ok, null);

        Intent finalAppLaunchIntent = appLaunchIntent;
        if (appLaunchIntent != null)
            builder.setPositiveButton(R.string.installer_open, (d, w) -> {
                try {
                    startActivity(finalAppLaunchIntent);
                } catch (ActivityNotFoundException e) {
                    Log.w("AppInstalledDialog", "Unable to launch activity", e);
                    SimpleAlertDialogFragment.newInstance(getString(R.string.error), getString(R.string.installer_unable_to_launch_app)).show(getParentFragmentManager(), null);
                }
                dismiss();
            });

        return builder.create();
    }
}
