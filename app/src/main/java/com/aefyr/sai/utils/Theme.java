package com.aefyr.sai.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class Theme {
    private static Theme sInstance;

    private SharedPreferences mPrefs;

    public static Theme getInstance(Context c) {
        return sInstance != null ? sInstance : new Theme(c);
    }

    private Theme(Context c) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(c);
        sInstance = this;
    }

    public boolean isDark() {
        return mPrefs.getBoolean(PreferencesKeys.DARK_THEME, false);
    }

    public void setDark(boolean dark) {
        mPrefs.edit().putBoolean(PreferencesKeys.DARK_THEME, dark).apply();
    }
}
