package com.aefyr.sai.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.aefyr.sai.R;

import java.util.Objects;

public class SimpleAlertDialogFragment extends DialogFragment {
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";

    private CharSequence mTitle;
    private CharSequence mMessage;

    public static SimpleAlertDialogFragment newInstance(Context c, @StringRes int title, @StringRes int message) {
        return newInstance(c.getText(title), c.getText(message));
    }

    public static SimpleAlertDialogFragment newInstance(CharSequence title, CharSequence message) {
        SimpleAlertDialogFragment fragment = new SimpleAlertDialogFragment();
        Bundle args = new Bundle();
        args.putCharSequence(ARG_TITLE, title);
        args.putCharSequence(ARG_MESSAGE, message);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args == null)
            return;

        mTitle = args.getCharSequence(ARG_TITLE, "title");
        mMessage = args.getCharSequence(ARG_MESSAGE, "message");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(Objects.requireNonNull(getContext())).setTitle(mTitle).setMessage(mMessage).setPositiveButton(R.string.ok, null).create();
    }
}
