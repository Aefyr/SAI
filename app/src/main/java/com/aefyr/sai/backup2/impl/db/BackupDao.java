package com.aefyr.sai.backup2.impl.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import java.util.List;

@Dao
public interface BackupDao {

    @Query("SELECT * FROM BackupMetaEntity WHERE storage_id = :storageId AND uri = :uri")
    BackupMetaEntity getBackupMetaForUri(String storageId, String uri);

    @Query("DELETE FROM BackupMetaEntity WHERE storage_id = :storageId AND uri = :uri")
    void removeByUri(String storageId, String uri);

    @Insert
    void add(BackupMetaEntity entity);

    @Update
    void update(BackupMetaEntity entity);

    @Query("SELECT * FROM BackupMetaEntity WHERE package = :pkg ORDER BY export_timestamp DESC LIMIT 1")
    BackupMetaEntity getLatestBackupForPackage(String pkg);

    @Query("SELECT DISTINCT package FROM BackupMetaEntity")
    List<String> getAllPackages();

    @Query("SELECT * FROM BackupMetaEntity WHERE package = :pkg")
    List<BackupMetaEntity> getAllBackupsForPackage(String pkg);

    @Query("SELECT * FROM BackupMetaEntity WHERE package = :pkg")
    LiveData<List<BackupMetaEntity>> getAllBackupsForPackageLiveData(String pkg);

    @Query("DELETE FROM BackupMetaEntity")
    void dropAllEntries();

    @Transaction
    default void runInTransaction(Runnable runnable) {
        runnable.run();
    }
}
