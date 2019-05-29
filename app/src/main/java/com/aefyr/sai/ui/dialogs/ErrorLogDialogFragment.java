package com.aefyr.sai.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.aefyr.sai.R;
import com.aefyr.sai.utils.Utils;

import java.util.Objects;

public class ErrorLogDialogFragment extends DialogFragment {
    private static final String ARG_TITLE = "title";
    private static final String ARG_LOG = "log";

    private CharSequence mTitle;
    private CharSequence mLog;

    public static ErrorLogDialogFragment newInstance(CharSequence title, CharSequence errorLog) {
        ErrorLogDialogFragment fragment = new ErrorLogDialogFragment();
        Bundle args = new Bundle();
        args.putCharSequence(ARG_TITLE, title);
        args.putCharSequence(ARG_LOG, errorLog);
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
        mLog = args.getCharSequence(ARG_LOG, "log");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(Objects.requireNonNull(getContext()))
                .setTitle(mTitle)
                .setMessage(mLog)
                .setPositiveButton(R.string.ok, null)
                .setNeutralButton(R.string.copy, (d, w) -> {
                    Utils.copyTextToClipboard(getContext(), mLog);
                    Toast.makeText(getContext(), R.string.copied, Toast.LENGTH_SHORT).show();
                    dismiss();
                })
                .create();
    }
}
