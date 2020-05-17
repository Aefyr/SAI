package com.aefyr.sai.backup2.impl.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface BackupDao {

    @Transaction
    @Query("SELECT * FROM BackupEntity WHERE uri = :uri")
    BackupWithComponents getBackupMetaForUri(String uri);

    @Query("DELETE FROM BackupEntity WHERE uri = :uri")
    void removeByUri(String uri);

    @Insert
    void insertBackup(BackupEntity backupEntity);

    @Insert
    void insertBackupComponent(BackupComponentEntity componentEntity);

    @Transaction
    @Query("SELECT * FROM BackupEntity WHERE package = :pkg ORDER BY export_timestamp DESC LIMIT 1")
    BackupWithComponents getLatestBackupForPackage(String pkg);

    @Query("SELECT DISTINCT package FROM BackupEntity")
    List<String> getAllPackages();

    @Transaction
    @Query("SELECT * FROM BackupEntity WHERE package = :pkg ORDER BY export_timestamp DESC")
    List<BackupWithComponents> getAllBackupsForPackage(String pkg);

    @Transaction
    @Query("SELECT * FROM BackupEntity WHERE package = :pkg ORDER BY export_timestamp DESC")
    LiveData<List<BackupWithComponents>> getAllBackupsForPackageLiveData(String pkg);

    @Query("DELETE FROM BackupEntity")
    void dropAllEntries();

    @Transaction
    default void runInTransaction(Runnable runnable) {
        runnable.run();
    }
}
