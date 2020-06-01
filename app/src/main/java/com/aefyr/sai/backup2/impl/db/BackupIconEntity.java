package com.aefyr.sai.backup2.impl.db;

import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;

import java.io.File;

@Entity(
        indices = {@Index(value = {"id", "session_id", "icon_file"})},
        primaryKeys = "id",
        foreignKeys = {@ForeignKey(entity = BackupEntity.class,
                parentColumns = {"icon_id"},
                childColumns = {"id"},
                onDelete = ForeignKey.CASCADE,
                onUpdate = ForeignKey.CASCADE)}
)
public class BackupIconEntity {

    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "session_id")
    public String sessionId;

    @ColumnInfo(name = "icon_file")
    public String iconFile;

    public static BackupIconEntity create(String id, String sessionId, File iconFile) {
        BackupIconEntity entity = new BackupIconEntity();

        entity.id = id;
        entity.sessionId = sessionId;
        entity.iconFile = iconFile.getAbsolutePath();

        return entity;
    }

    public Uri iconUri() {
        return Uri.fromFile(iconFile());
    }

    public File iconFile() {
        return new File(iconFile);
    }
}
