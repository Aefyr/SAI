package com.aefyr.sai.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.aefyr.sai.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class InstallationConfirmationDialogFragment extends DialogFragment {
    private static final String ARG_APKS_FILE = "file";

    public interface ConfirmationListener {
        void onConfirmed(Uri uri);
    }

    private Uri mApksFileUri;

    private ConfirmationListener mListener;

    public static InstallationConfirmationDialogFragment newInstance(Uri apksFileUri) {
        InstallationConfirmationDialogFragment fragment = new InstallationConfirmationDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_APKS_FILE, apksFileUri);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args == null)
            return;
        mApksFileUri = args.getParcelable(ARG_APKS_FILE);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getContext())
                .setMessage(getString(R.string.installer_installation_confirmation, mApksFileUri.getLastPathSegment()))
                .setPositiveButton(R.string.yes, (d, w) -> {
                    mListener.onConfirmed(mApksFileUri);
                    dismiss();
                })
                .setNegativeButton(R.string.cancel, (d, w) -> dismiss())
                .create();
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        try {
            if (getParentFragment() != null)
                mListener = (ConfirmationListener) getParentFragment();
            else
                mListener = (ConfirmationListener) getActivity();
        } catch (Exception e) {
            throw new IllegalStateException("Activity/Fragment that uses InstallationConfirmationDialogFragment must implement InstallationConfirmationDialogFragment.ConfirmationListener");
        }
    }
}
