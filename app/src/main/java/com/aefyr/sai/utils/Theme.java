package com.aefyr.sai.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import androidx.annotation.StringRes;
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
        mThemes.add(new ThemeDescriptor(0, R.style.AppTheme_Light, false, R.string.theme_sai, false));
        mThemes.add(new ThemeDescriptor(1, R.style.AppTheme_Dark, true, R.string.theme_ruby, false));
        mThemes.add(new ThemeDescriptor(2, R.style.AppTheme_Rena, true, R.string.theme_rena, false));
        mThemes.add(new ThemeDescriptor(3, R.style.AppTheme_Rooter, false, R.string.theme_ukrrooter, false));
        mThemes.add(new ThemeDescriptor(4, R.style.AppTheme_Omelette, true, R.string.theme_amoled, false));
        mThemes.add(new ThemeDescriptor(5, R.style.AppTheme_Pixel, false, R.string.theme_pixel, false));
        mThemes.add(new ThemeDescriptor(6, R.style.AppTheme_FDroid, false, R.string.theme_sai_fdroid, false));
        mThemes.add(new ThemeDescriptor(7, R.style.AppTheme_Dark2, true, R.string.theme_dark, false));
        mThemes.add(new ThemeDescriptor(8, R.style.AppTheme_Gold, true, R.string.theme_gold, true));

        mPrefs = PreferenceManager.getDefaultSharedPreferences(c);
        sInstance = this;
    }

    private int getCurrentThemeId() {
        return mPrefs.getInt(PreferencesKeys.CURRENT_THEME, BuildConfig.DEFAULT_THEME);
    }

    public void setCurrentTheme(ThemeDescriptor theme) {
        mPrefs.edit().putInt(PreferencesKeys.CURRENT_THEME, theme.getId()).apply();
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

    public List<ThemeDescriptor> getThemes() {
        return mThemes;
    }

    public static class ThemeDescriptor {
        private int mId;

        @StyleRes
        private int mTheme;
        private boolean mIsDark;

        @StringRes
        private int mNameStringRes;
        private boolean mDonationRequired;

        private ThemeDescriptor(int id, @StyleRes int theme, boolean isDark, @StringRes int nameStringRes, boolean donationRequired) {
            mId = id;
            mTheme = theme;
            mIsDark = isDark;
            mNameStringRes = nameStringRes;
            mDonationRequired = donationRequired;
        }

        public int getId() {
            return mId;
        }

        @StyleRes
        public int getTheme() {
            return mTheme;
        }

        public boolean isDark() {
            return mIsDark;
        }

        public String getName(Context c) {
            return c.getString(mNameStringRes);
        }

        public boolean isDonationRequired() {
            return mDonationRequired;
        }
    }
}
