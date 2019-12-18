package com.aefyr.sai.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;

import com.github.angads25.filepicker.model.DialogConfigs;

import java.io.File;

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

    public int getFilePickerRawSort() {
        return mPrefs.getInt(PreferencesKeys.FILE_PICKER_SORT_RAW, 0);
    }

    public void setFilePickerRawSort(int rawSort) {
        mPrefs.edit().putInt(PreferencesKeys.FILE_PICKER_SORT_RAW, rawSort).apply();
    }

    public int getFilePickerSortBy() {
        return mPrefs.getInt(PreferencesKeys.FILE_PICKER_SORT_BY, DialogConfigs.SORT_BY_NAME);
    }

    public void setFilePickerSortBy(int sortBy) {
        mPrefs.edit().putInt(PreferencesKeys.FILE_PICKER_SORT_BY, sortBy).apply();
    }

    public int getFilePickerSortOrder() {
        return mPrefs.getInt(PreferencesKeys.FILE_PICKER_SORT_ORDER, DialogConfigs.SORT_ORDER_NORMAL);
    }

    public void setFilePickerSortOrder(int sortOrder) {
        mPrefs.edit().putInt(PreferencesKeys.FILE_PICKER_SORT_ORDER, sortOrder).apply();
    }

    public boolean shouldSignApks() {
        return mPrefs.getBoolean(PreferencesKeys.SIGN_APKS, false);
    }

    public void setShouldSignApks(boolean signApks) {
        mPrefs.edit().putBoolean(PreferencesKeys.SIGN_APKS, signApks).apply();
    }

    public boolean shouldExtractArchives() {
        return mPrefs.getBoolean(PreferencesKeys.EXTRACT_ARCHIVES, false);
    }

    public void setInstaller(int installer) {
        mPrefs.edit().putInt(PreferencesKeys.INSTALLER, installer).apply();
    }

    public int getInstaller() {
        return mPrefs.getInt(PreferencesKeys.INSTALLER, PreferencesValues.INSTALLER_ROOTLESS);
    }

    public void setBackupFileNameFormat(String format) {
        mPrefs.edit().putString(PreferencesKeys.BACKUP_FILE_NAME_FORMAT, format).apply();
    }

    public String getBackupFileNameFormat() {
        return mPrefs.getString(PreferencesKeys.BACKUP_FILE_NAME_FORMAT, PreferencesValues.BACKUP_FILE_NAME_FORMAT_DEFAULT);
    }

    public void setInstallLocation(int installLocation) {
        mPrefs.edit().putString(PreferencesKeys.INSTALL_LOCATION, String.valueOf(installLocation)).apply();
    }

    public int getInstallLocation() {
        String rawInstallLocation = mPrefs.getString(PreferencesKeys.INSTALL_LOCATION, "0");
        try {
            return Integer.parseInt(rawInstallLocation);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public boolean useOldInstaller() {
        return mPrefs.getBoolean(PreferencesKeys.USE_OLD_INSTALLER, false);
    }

    public boolean showInstallerDialogs() {
        return mPrefs.getBoolean(PreferencesKeys.SHOW_INSTALLER_DIALOGS, true);
    }

    public Uri getBackupDirUri() {
        String rawBackupDirUri = mPrefs.getString(PreferencesKeys.BACKUP_DIR, null);
        if (rawBackupDirUri == null)
            return Uri.fromFile(new File(Environment.getExternalStorageDirectory(), "SAI"));

        return Uri.parse(rawBackupDirUri);
    }

    public void setBackupDirUri(String uri) {
        mPrefs.edit().putString(PreferencesKeys.BACKUP_DIR, uri).apply();
    }

}
