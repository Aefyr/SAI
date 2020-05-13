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

    @Query("SELECT * FROM BackupEntity WHERE storage_id = :storageId AND uri = :uri")
    BackupEntity getBackupMetaForUri(String storageId, String uri);

    @Query("DELETE FROM BackupEntity WHERE storage_id = :storageId AND uri = :uri")
    void removeByUri(String storageId, String uri);

    @Insert
    void add(BackupEntity entity);

    @Update
    void update(BackupEntity entity);

    @Query("SELECT * FROM BackupEntity WHERE package = :pkg ORDER BY export_timestamp DESC LIMIT 1")
    BackupEntity getLatestBackupForPackage(String pkg);

    @Query("SELECT DISTINCT package FROM BackupEntity")
    List<String> getAllPackages();

    @Query("SELECT * FROM BackupEntity WHERE package = :pkg ORDER BY export_timestamp DESC")
    List<BackupEntity> getAllBackupsForPackage(String pkg);

    @Query("SELECT * FROM BackupEntity WHERE package = :pkg ORDER BY export_timestamp DESC")
    LiveData<List<BackupEntity>> getAllBackupsForPackageLiveData(String pkg);

    @Query("DELETE FROM BackupEntity")
    void dropAllEntries();

    @Transaction
    default void runInTransaction(Runnable runnable) {
        runnable.run();
    }
}
