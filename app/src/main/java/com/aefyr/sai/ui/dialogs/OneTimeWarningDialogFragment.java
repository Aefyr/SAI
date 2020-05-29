package com.aefyr.sai.ui.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

import com.aefyr.sai.R;

public class OneTimeWarningDialogFragment extends DialogFragment {
    private static final String ARG_TITLE = "title";
    private static final String ARG_MESSAGE = "message";
    private static final String ARG_TAG = "tag";
    private static final String PREFS_NAME = "one_time_warnings";

    private CharSequence mTitle;
    private CharSequence mMessage;
    private String mTag;

    /**
     * @param tag unique tag for this warning
     */
    public static void showIfNeeded(Context context, FragmentManager fragmentManager, @StringRes int title, @StringRes int message, String tag) {
        showIfNeeded(context, fragmentManager, context.getText(title), context.getText(message), tag);
    }

    /**
     * @param tag unique tag for this warning
     */
    public static void showIfNeeded(Context context, FragmentManager fragmentManager, CharSequence title, CharSequence message, String tag) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (prefs.getBoolean(tag, false))
            return;

        newInstance(title, message, tag).show(fragmentManager, null);
    }

    /**
     * @param tag unique tag for this warning
     */
    private static OneTimeWarningDialogFragment newInstance(CharSequence title, CharSequence message, String tag) {
        Bundle args = new Bundle();
        args.putCharSequence(ARG_TITLE, title);
        args.putCharSequence(ARG_MESSAGE, message);
        args.putString(ARG_TAG, tag);

        OneTimeWarningDialogFragment fragment = new OneTimeWarningDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args == null)
            throw new IllegalStateException("Arguments mustn't be null");

        mTitle = args.getCharSequence(ARG_TITLE, "title");
        mMessage = args.getCharSequence(ARG_MESSAGE, "message");
        mTag = args.getString(ARG_TAG);

        setCancelable(false);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(requireContext())
                .setTitle(mTitle)
                .setMessage(mMessage)
                .setPositiveButton(R.string.dont_show_again, (d, w) -> requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().putBoolean(mTag, true).apply())
                .create();
    }


}
