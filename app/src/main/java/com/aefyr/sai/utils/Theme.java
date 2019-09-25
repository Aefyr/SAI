package com.aefyr.sai.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.StyleRes;

import com.aefyr.sai.BuildConfig;
import com.aefyr.sai.R;

import java.util.ArrayList;
import java.util.List;

public class Theme {
    private static Theme sInstance;

    private SharedPreferences mPrefs;

    private List<ThemeDescriptor> mThemes;

    public static Theme getInstance(Context c) {
        synchronized (Theme.class) {
            return sInstance != null ? sInstance : new Theme(c);
        }
    }

    private Theme(Context c) {
        mThemes = new ArrayList<>();
        mThemes.add(new ThemeDescriptor(R.style.AppTheme_Light, false));
        mThemes.add(new ThemeDescriptor(R.style.AppTheme_Dark, true));
        mThemes.add(new ThemeDescriptor(R.style.AppTheme_Rena, true));
        mThemes.add(new ThemeDescriptor(R.style.AppTheme_Rooter, false));
        mThemes.add(new ThemeDescriptor(R.style.AppTheme_Omelette, true));
        mThemes.add(new ThemeDescriptor(R.style.AppTheme_Pixel, false));
        mThemes.add(new ThemeDescriptor(R.style.AppTheme_FDroid, false));
        mThemes.add(new ThemeDescriptor(R.style.AppTheme_Dark2, true));

        mPrefs = PreferenceManager.getDefaultSharedPreferences(c);
        sInstance = this;
    }

    public int getCurrentThemeId() {
        return mPrefs.getInt(PreferencesKeys.CURRENT_THEME, BuildConfig.DEFAULT_THEME);
    }

    public void setCurrentTheme(int themeId) {
        mPrefs.edit().putInt(PreferencesKeys.CURRENT_THEME, themeId).apply();
    }

    public static void apply(Context c) {
        c.setTheme(getInstance(c).getCurrentTheme());
    }

    @StyleRes
    public int getCurrentTheme() {
        return getCurrentThemeDescriptor().getTheme();
    }

    public ThemeDescriptor getCurrentThemeDescriptor() {
        if (getCurrentThemeId() >= mThemes.size())
            return mThemes.get(0);

        return mThemes.get(getCurrentThemeId());
    }

    public static class ThemeDescriptor {
        @StyleRes
        private int mTheme;
        private boolean mIsDark;

        private ThemeDescriptor(@StyleRes int theme, boolean isDark) {
            mTheme = theme;
            mIsDark = isDark;
        }

        @StyleRes
        public int getTheme() {
            return mTheme;
        }

        public boolean isDark() {
            return mIsDark;
        }
    }
}
