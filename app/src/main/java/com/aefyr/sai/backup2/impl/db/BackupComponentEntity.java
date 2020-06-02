package com.aefyr.sai.backup2.impl.db;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import com.aefyr.sai.backup2.BackupComponent;

@Entity(
        indices = {@Index(value = {"backup_uri"})},
        primaryKeys = {"backup_uri", "type"},
        foreignKeys = {@ForeignKey(entity = BackupEntity.class,
                parentColumns = {"uri"},
                childColumns = {"backup_uri"},
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE)}
)
public class BackupComponentEntity implements BackupComponent {

    @NonNull
    @ColumnInfo(name = "backup_uri")
    String backupUri;

    @NonNull
    @ColumnInfo(name = "type")
    String type;

    @ColumnInfo(name = "size")
    long size;

    public static BackupComponentEntity fromBackupComponent(Uri backupUri, BackupComponent backupComponent) {
        BackupComponentEntity componentEntity = new BackupComponentEntity();
        componentEntity.backupUri = backupUri.toString();
        componentEntity.type = backupComponent.type();
        componentEntity.size = backupComponent.size();

        return componentEntity;
    }


    @Override
    public String type() {
        return type;
    }

    @Override
    public long size() {
        return size;
    }
}
