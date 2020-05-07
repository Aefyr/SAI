package com.aefyr.sai.backup2;

import android.net.Uri;
import android.os.Handler;

import java.util.List;

public interface BackupStorage {

    String getStorageId();

    /**
     * List backup files in this storage
     *
     * @return
     */
    List<Uri> listBackupFiles();

    /**
     * Get some kind of identifier for backup file content
     *
     * @param uri
     * @return
     */
    String getBackupFileHash(Uri uri);

    /**
     * Retrieve package meta for given backup file
     *
     * @param uri
     * @return
     */
    BackupFileMeta getMetaForBackupFile(Uri uri) throws Exception;


    void backupApp(BackupTaskConfig config, String tag);

    void addBackupProgressListener(BackupProgressListener progressListener, Handler progressListenerHandler);

    void removeBackupProgressListener(BackupProgressListener progressListener);

    void addObserver(Observer observer, Handler observerHandler);

    void removeObserver(Observer observer);

    interface Observer {

        void onBackupAdded(BackupFileMeta meta);

        void onBackupRemoved(BackupFileMeta meta);

        void onStorageUpdated();

    }

    interface BackupProgressListener {

        void onProgressChanged(String tag, long current, long goal);

        void onBackupCompleted(String tag, BackupFileMeta backupFileMeta);

        void onBackupFailed(String tag, Exception e);
    }
}
