package com.aefyr.sai.backup2.impl.db;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;

import com.aefyr.sai.backup2.Backup;

@Entity(
        indices = {@Index(value = {"package", "uri", "content_hash"}), @Index(unique = true, value = {"icon_id"})},
        primaryKeys = {"uri"}
)
public class BackupEntity {

    public static final long FLAG_SPLIT_APK = 0x1;

    @NonNull
    @ColumnInfo(name = "uri")
    public String uri;

    @ColumnInfo(name = "package")
    public String pkg;

    @ColumnInfo(name = "label")
    public String label;

    @ColumnInfo(name = "version_name")
    public String versionName;

    @ColumnInfo(name = "version_code")
    public long versionCode;

    @ColumnInfo(name = "export_timestamp")
    public long exportTimestamp;

    @ColumnInfo(name = "icon_id")
    public String iconId;

    @NonNull
    @ColumnInfo(name = "content_hash")
    public String contentHash;

    @NonNull
    @ColumnInfo(name = "storage_id")
    public String storageId;

    @NonNull
    @ColumnInfo(name = "flags")
    public long flags;

    public Uri getUri() {
        return Uri.parse(uri);
    }

    public boolean isSplitApk() {
        return hasFlag(FLAG_SPLIT_APK);
    }

    private boolean hasFlag(long flag) {
        return (flags & flag) == flag;
    }

    private void addFlag(long flag) {
        flags = flags | flag;
    }

    public static BackupEntity fromBackup(Backup backup, String iconId) {
        BackupEntity backupEntity = new BackupEntity();

        backupEntity.uri = backup.uri().toString();
        backupEntity.pkg = backup.pkg();
        backupEntity.label = backup.appName();
        backupEntity.versionName = backup.versionName();
        backupEntity.versionCode = backup.versionCode();
        backupEntity.exportTimestamp = backup.creationTime();
        backupEntity.iconId = iconId;
        backupEntity.contentHash = backup.contentHash();
        backupEntity.storageId = backup.storageId();

        if (backup.isSplitApk())
            backupEntity.addFlag(FLAG_SPLIT_APK);

        return backupEntity;
    }
}
