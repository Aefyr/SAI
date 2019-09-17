package com.aefyr.sai.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.SparseIntArray;

import androidx.annotation.StyleRes;

import com.aefyr.sai.BuildConfig;
import com.aefyr.sai.R;

public class Theme {
    private static Theme sInstance;

    private SharedPreferences mPrefs;

    private SparseIntArray mThemes;

    public static Theme getInstance(Context c) {
        synchronized (Theme.class) {
            return sInstance != null ? sInstance : new Theme(c);
        }
    }

    private Theme(Context c) {
        mThemes = new SparseIntArray();
        mThemes.append(0, R.style.AppTheme_Light);
        mThemes.append(1, R.style.AppTheme_Dark);
        mThemes.append(2, R.style.AppTheme_Rena);
        mThemes.append(3, R.style.AppTheme_Rooter);
        mThemes.append(4, R.style.AppTheme_Omelette);
        mThemes.append(5, R.style.AppTheme_Pixel);
        mThemes.append(6, R.style.AppTheme_FDroid);
        mThemes.append(7, R.style.AppTheme_Dark2);

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
        return mThemes.get(getCurrentThemeId(), R.style.AppTheme_Light);
    }
}
