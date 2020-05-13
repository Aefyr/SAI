package com.aefyr.sai.backup2;

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
}
