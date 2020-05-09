package com.aefyr.sai.backup2;

import android.net.Uri;
import android.os.Handler;

import androidx.annotation.Nullable;

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

    /**
     * @param config
     * @return backup task token
     */
    String createBackupTask(BackupTaskConfig config);

    void startBackupTask(String taskToken);

    void cancelBackupTask(String taskToken);

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

        void onBackupTaskStatusChanged(BackupTaskStatus status);

    }

    enum BackupTaskState {
        CREATED, QUEUED, IN_PROGRESS, CANCELLED, SUCCEEDED, FAILED
    }

    class BackupTaskStatus {

        private String mToken;
        private BackupTaskConfig mConfig;
        private BackupTaskState mState;
        private long mCurrentProgress;
        private long mGoal;
        private BackupFileMeta mMeta;
        private Exception mException;

        private BackupTaskStatus(String token, BackupTaskConfig config, BackupTaskState state, long currentProgress, long goal, @Nullable BackupFileMeta meta, @Nullable Exception exception) {
            mToken = token;
            mConfig = config;
            mState = state;
            mCurrentProgress = currentProgress;
            mGoal = goal;
            mMeta = meta;
            mException = exception;
        }

        public static BackupTaskStatus created(String token, BackupTaskConfig config) {
            return new BackupTaskStatus(token, config, BackupTaskState.CREATED, 0, 0, null, null);
        }

        public static BackupTaskStatus queued(String token, BackupTaskConfig config) {
            return new BackupTaskStatus(token, config, BackupTaskState.QUEUED, 0, 0, null, null);
        }

        public static BackupTaskStatus inProgress(String token, BackupTaskConfig config, long currentProgress, long goal) {
            return new BackupTaskStatus(token, config, BackupTaskState.IN_PROGRESS, currentProgress, goal, null, null);
        }

        public static BackupTaskStatus cancelled(String token, BackupTaskConfig config) {
            return new BackupTaskStatus(token, config, BackupTaskState.CANCELLED, 0, 0, null, null);
        }

        public static BackupTaskStatus succeeded(String token, BackupTaskConfig config, BackupFileMeta meta) {
            return new BackupTaskStatus(token, config, BackupTaskState.SUCCEEDED, 0, 0, meta, null);
        }

        public static BackupTaskStatus failed(String token, BackupTaskConfig config, Exception e) {
            return new BackupTaskStatus(token, config, BackupTaskState.FAILED, 0, 0, null, e);
        }

        public String token() {
            return mToken;
        }

        public BackupTaskConfig config() {
            return mConfig;
        }

        public BackupTaskState state() {
            return mState;
        }

        public long currentProgress() {
            return mCurrentProgress;
        }

        public long progressGoal() {
            return mGoal;
        }

        public BackupFileMeta meta() {
            return mMeta;
        }

        public Exception exception() {
            return mException;
        }


    }
}
