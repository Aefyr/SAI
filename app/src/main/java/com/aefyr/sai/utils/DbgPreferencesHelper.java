package com.aefyr.sai.utils;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

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

    public String getCustomInstallCreateCommand() {
        String command = mPrefs.getString(DbgPreferencesKeys.CUSTOM_INSTALL_CREATE, "null");
        if ("null".equalsIgnoreCase(command))
            return null;

        return command;
    }

    public boolean addFakeTimestampToBackups() {
        return mPrefs.getBoolean(DbgPreferencesKeys.ADD_FAKE_TIMESTAMP_TO_BACKUPS, false);
    }
}
