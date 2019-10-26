package com.aefyr.sai.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class DbgPreferencesHelper {
    private static DbgPreferencesHelper sInstance;

    private SharedPreferences mPrefs;

    public static DbgPreferencesHelper getInstance(Context c) {
        return sInstance != null ? sInstance : new DbgPreferencesHelper(c);
    }

    private DbgPreferencesHelper(Context c) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(c);
        sInstance = this;
    }

    public boolean shouldReplaceDots() {
        return !mPrefs.getBoolean(DbgPreferencesKeys.DONT_REPLACE_DOTS, false);
    }
}
