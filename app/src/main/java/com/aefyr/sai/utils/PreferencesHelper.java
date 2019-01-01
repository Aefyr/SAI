package com.aefyr.sai.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;

public class PreferencesHelper {
    private static PreferencesHelper sInstance;

    private SharedPreferences mPrefs;

    public static PreferencesHelper getInstance(Context c) {
        return sInstance != null ? sInstance : new PreferencesHelper(c);
    }

    private PreferencesHelper(Context c) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(c);
        sInstance = this;
    }

    public String getHomeDirectory() {
        return mPrefs.getString(PreferencesKeys.HOME_DIRECTORY, Environment.getExternalStorageDirectory().getAbsolutePath());
    }

    public void setHomeDirectory(String homeDirectory) {
        mPrefs.edit().putString(PreferencesKeys.HOME_DIRECTORY, homeDirectory).apply();
    }

    public boolean shouldUseRoot() {
        return mPrefs.getBoolean(PreferencesKeys.USE_ROOT, false);
    }

    public void setShouldUseRoot(boolean useRoot) {
        mPrefs.edit().putBoolean(PreferencesKeys.USE_ROOT, useRoot).apply();
    }
}
