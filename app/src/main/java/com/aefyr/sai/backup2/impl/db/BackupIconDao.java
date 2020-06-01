package com.aefyr.sai.backup2.impl.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface BackupIconDao {

    @Insert
    void addIcon(BackupIconEntity backupIconEntity);

    @Query("SELECT EXISTS (SELECT 1 FROM BackupIconEntity WHERE session_id = :sessionId AND icon_file = :iconFile)")
    boolean containsIcon(String sessionId, String iconFile);

    @Query("DELETE FROM BackupIconEntity")
    void drop();

}
