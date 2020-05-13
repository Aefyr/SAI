package com.aefyr.sai.backup2;

import androidx.annotation.DrawableRes;

import com.aefyr.sai.R;

public enum BackupStatus {
    NO_BACKUP, SAME_VERSION, HIGHER_VERSION, LOWER_VERSION, APP_NOT_INSTALLED;

    public static BackupStatus fromInstalledAppAndBackupVersions(long installedAppVersion, long backupVersion) {
        if (backupVersion == installedAppVersion)
            return BackupStatus.SAME_VERSION;
        else if (backupVersion > installedAppVersion)
            return BackupStatus.HIGHER_VERSION;
        else
            return BackupStatus.LOWER_VERSION;
    }

    @DrawableRes
    public int getIconRes() {
        switch (this) {
            case NO_BACKUP:
                return R.drawable.ic_backup_status_no_backup;
            case SAME_VERSION:
                return R.drawable.ic_backup_status_same_version;
            case HIGHER_VERSION:
                return R.drawable.ic_backup_status_higher_version;
            case LOWER_VERSION:
                return R.drawable.ic_backup_status_lower_version;
            case APP_NOT_INSTALLED:
                return R.drawable.ic_backup_status_not_installed;
        }

        throw new RuntimeException("wtf");
    }
}
