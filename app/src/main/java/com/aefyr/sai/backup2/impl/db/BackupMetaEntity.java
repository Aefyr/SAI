package com.aefyr.sai.backup2.impl.db;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;

import com.aefyr.sai.backup2.BackupFileMeta;
import com.aefyr.sai.model.common.PackageMeta;

@Entity(
        indices = {@Index(value = {"package", "uri", "content_hash"})},
        primaryKeys = {"uri", "storage_id"}
)
public class BackupMetaEntity {

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

    @ColumnInfo(name = "icon_uri")
    public String iconUri;

    @NonNull
    @ColumnInfo(name = "content_hash")
    public String contentHash;

    @NonNull
    @ColumnInfo(name = "storage_id")
    public String storageId;

    public Uri getUri() {
        return Uri.parse(uri);
    }

    public Uri getIconUri() {
        return Uri.parse(iconUri);
    }

    public static BackupMetaEntity fromBackupFileMeta(BackupFileMeta meta) {
        BackupMetaEntity backupMetaEntity = new BackupMetaEntity();

        backupMetaEntity.uri = meta.uri.toString();
        backupMetaEntity.pkg = meta.pkg;
        backupMetaEntity.label = meta.label;
        backupMetaEntity.versionName = meta.versionName;
        backupMetaEntity.versionCode = meta.versionCode;
        backupMetaEntity.exportTimestamp = meta.exportTimestamp;
        backupMetaEntity.iconUri = meta.iconUri.toString();
        backupMetaEntity.contentHash = meta.contentHash;
        backupMetaEntity.storageId = meta.storageId;

        return backupMetaEntity;
    }

    public BackupFileMeta toBackupFileMeta() {
        BackupFileMeta backupFileMeta = new BackupFileMeta();

        backupFileMeta.uri = getUri();
        backupFileMeta.pkg = pkg;
        backupFileMeta.label = label;
        backupFileMeta.versionCode = versionCode;
        backupFileMeta.versionName = versionName;
        backupFileMeta.exportTimestamp = exportTimestamp;
        backupFileMeta.iconUri = getIconUri();
        backupFileMeta.contentHash = contentHash;
        backupFileMeta.storageId = storageId;

        return backupFileMeta;
    }

    public PackageMeta toPackageMeta() {
        return new PackageMeta.Builder(pkg)
                .setLabel(label)
                .setVersionCode(versionCode)
                .setVersionName(versionName)
                .setIconUri(Uri.parse(iconUri))
                .build();
    }

}
