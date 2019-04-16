package com.aefyr.sai.ui.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import com.aefyr.sai.R;
import com.aefyr.sai.utils.Theme;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class ThemeSelectionDialogFragment extends DialogFragment {

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getContext())
                .setTitle(R.string.installer_select_theme)
                .setSingleChoiceItems(R.array.themes, Theme.getInstance(getContext()).getCurrentThemeId(), (d, w) -> {
                    Theme.getInstance(getContext()).setCurrentTheme(w);
                    dismiss();
                    getActivity().recreate();
                })
                .create();
    }
}
