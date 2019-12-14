package com.aefyr.sai.ui.dialogs;

import android.content.Context;
import android.os.Bundle;

import com.aefyr.sai.R;
import com.aefyr.sai.utils.Theme;

public class ThemeSelectionDialogFragment extends SingleChoiceListDialogFragment {

    public static ThemeSelectionDialogFragment newInstance(Context context) {
        ThemeSelectionDialogFragment fragment = new ThemeSelectionDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_PARAMS, new DialogParams("whatever", context.getString(R.string.installer_select_theme), R.array.themes, Theme.getInstance(context).getCurrentThemeId()));

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void deliverSelectionResult(String tag, int selectedItemIndex) {
        Theme.getInstance(getContext()).setCurrentTheme(selectedItemIndex);
        requireActivity().recreate();
        dismiss();
    }
}

