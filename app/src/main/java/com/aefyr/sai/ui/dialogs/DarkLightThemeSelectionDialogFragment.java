package com.aefyr.sai.ui.dialogs;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.IntDef;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.aefyr.sai.R;
import com.aefyr.sai.ui.dialogs.base.BaseBottomSheetDialogFragment;
import com.aefyr.sai.utils.Theme;
import com.aefyr.sai.utils.Utils;
import com.aefyr.sai.view.ThemeView;
import com.aefyr.sai.viewmodels.DarkLightThemeSelectionViewModel;

public class DarkLightThemeSelectionDialogFragment extends BaseBottomSheetDialogFragment implements ThemeSelectionDialogFragment.OnThemeChosenListener {
    private static final String TAG_CHOOSE_LIGHT_THEME = "choose_light";
    private static final String TAG_CHOOSE_DARK_THEME = "choose_dark";

    @IntDef(flag = true, value = {MODE_APPLY, MODE_CHOOSE})
    public @interface Mode {
    }

    public static final int MODE_APPLY = 0;
    public static final int MODE_CHOOSE = 1;

    private static final String EXTRA_MODE = "mode";

    private int mMode = MODE_CHOOSE;

    private DarkLightThemeSelectionViewModel mViewModel;

    /**
     * Same as {@link #newInstance(int)} with {@link #MODE_CHOOSE}
     *
     * @return
     */
    public static DarkLightThemeSelectionDialogFragment newInstance() {
        return newInstance(MODE_CHOOSE);
    }

    public static DarkLightThemeSelectionDialogFragment newInstance(@Mode int mode) {
        DarkLightThemeSelectionDialogFragment fragment = new DarkLightThemeSelectionDialogFragment();

        Bundle args = new Bundle();
        args.putInt(EXTRA_MODE, mode);

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args != null) {
            mMode = args.getInt(EXTRA_MODE, MODE_CHOOSE);
        }

        mViewModel = new ViewModelProvider(this).get(DarkLightThemeSelectionViewModel.class);
    }

    @Nullable
    @Override
    protected View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_dark_light_theme_selection, container, false);
    }

    @Override
    protected void onContentViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onContentViewCreated(view, savedInstanceState);

        setTitle(R.string.auto_theme_title);

        getNegativeButton().setOnClickListener(v -> dismiss());
        getPositiveButton().setOnClickListener(v -> {
            if (mMode == MODE_CHOOSE) {
                OnDarkLightThemesChosenListener listener = Utils.getParentAs(this, OnDarkLightThemesChosenListener.class);
                if (listener != null)
                    listener.onThemesChosen(getTag(), mViewModel.getLightTheme().getValue(), mViewModel.getDarkTheme().getValue());
            } else {
                Theme theme = Theme.getInstance(requireContext());
                theme.setLightTheme(mViewModel.getLightTheme().getValue());
                theme.setDarkTheme(mViewModel.getDarkTheme().getValue());
                requireActivity().recreate();
            }

            dismiss();
        });

        ThemeView lightThemeView = view.findViewById(R.id.themeview_dl_selection_light);
        ThemeView darkThemeView = view.findViewById(R.id.themeview_dl_selection_dark);

        lightThemeView.setMessage(R.string.auto_theme_selection_hint);
        darkThemeView.setMessage(R.string.auto_theme_selection_hint);

        lightThemeView.setOnClickListener(v -> ThemeSelectionDialogFragment.newInstance(ThemeSelectionDialogFragment.MODE_CHOOSE).show(getChildFragmentManager(), TAG_CHOOSE_LIGHT_THEME));
        darkThemeView.setOnClickListener(v -> ThemeSelectionDialogFragment.newInstance(ThemeSelectionDialogFragment.MODE_CHOOSE).show(getChildFragmentManager(), TAG_CHOOSE_DARK_THEME));

        mViewModel.getLightTheme().observe(this, lightThemeView::setTheme);
        mViewModel.getDarkTheme().observe(this, darkThemeView::setTheme);

        revealBottomSheet();
    }

    @Override
    public void onThemeChosen(@Nullable String tag, Theme.ThemeDescriptor theme) {
        if (TAG_CHOOSE_LIGHT_THEME.equals(tag)) {
            mViewModel.setLightTheme(theme);
        } else if (TAG_CHOOSE_DARK_THEME.equals(tag)) {
            mViewModel.setDarkTheme(theme);
        }
    }

    public interface OnDarkLightThemesChosenListener {
        void onThemesChosen(@Nullable String tag, Theme.ThemeDescriptor lightTheme, Theme.ThemeDescriptor darkTheme);
    }
}
