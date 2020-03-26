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

public class ErrorLogDialogFragment2 extends DialogFragment {
    private static final String ARG_TITLE = "title";
    private static final String ARG_ERROR_MESSAGE = "error_message";
    private static final String ARG_ERROR_FULL = "error_full";
    private static final String ARG_DISPLAY_FULL_ERROR = "display_full_error";

    private CharSequence mTitle;
    private CharSequence mErrorMessage;
    private CharSequence mFullError;
    private boolean mDisplayFullError;


    public static ErrorLogDialogFragment2 newInstance(CharSequence title, CharSequence errorMessage, CharSequence fullError, boolean displayFullError) {
        ErrorLogDialogFragment2 fragment = new ErrorLogDialogFragment2();

        Bundle args = new Bundle();
        args.putCharSequence(ARG_TITLE, title);
        args.putCharSequence(ARG_ERROR_MESSAGE, errorMessage);
        args.putCharSequence(ARG_ERROR_FULL, fullError);
        args.putBoolean(ARG_DISPLAY_FULL_ERROR, displayFullError);
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
        mErrorMessage = args.getCharSequence(ARG_ERROR_MESSAGE, "error");
        mFullError = args.getCharSequence(ARG_ERROR_FULL, "full");
        mDisplayFullError = args.getBoolean(ARG_DISPLAY_FULL_ERROR, false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(Objects.requireNonNull(getContext()))
                .setTitle(mTitle)
                .setMessage(mDisplayFullError ? mFullError : mErrorMessage)
                .setPositiveButton(R.string.ok, null);

        if (mDisplayFullError) {
            builder.setNeutralButton(R.string.copy2, (d, w) -> {
                Utils.copyTextToClipboard(getContext(), mFullError);
                Toast.makeText(getContext(), R.string.copied, Toast.LENGTH_SHORT).show();
                dismiss();
            });
        } else {
            builder.setNeutralButton(R.string.installer_show_full_error, (d, w) -> {
                newInstance(mTitle, mErrorMessage, mFullError, true).show(requireFragmentManager(), getTag());
                dismiss();
            });
        }

        return builder.create();

    }
}
