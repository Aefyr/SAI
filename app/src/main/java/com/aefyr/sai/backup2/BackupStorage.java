package com.aefyr.sai.backup2;

import android.net.Uri;
import android.os.Handler;

import androidx.annotation.Nullable;

import com.aefyr.sai.backup2.backuptask.config.BackupTaskConfig;
import com.aefyr.sai.backup2.backuptask.config.BatchBackupTaskConfig;
import com.aefyr.sai.backup2.backuptask.config.SingleBackupTaskConfig;
import com.aefyr.sai.model.apksource.ApkSource;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public interface BackupStorage {

    String getStorageId();

    /**
     * List backup files in this storage
     *
     * @return
     */
    List<Uri> listBackupFiles() throws Exception;

    /**
     * Get some kind of identifier for backup file content
     *
     * @param uri
     * @return
     */
    String getBackupFileHash(Uri uri) throws Exception;

    /**
     * Retrieve meta for given backup uri
     *
     * @param uri
     * @return
     */
    Backup getBackupByUri(Uri uri) throws Exception;

    InputStream getBackupIcon(Uri iconUri) throws Exception;

    /**
     * @param config
     * @return backup task token
     */
    String createBackupTask(SingleBackupTaskConfig config);

    /**
     * @param config
     * @return backup task token
     */
    String createBatchBackupTask(BatchBackupTaskConfig config);

    void startBackupTask(String taskToken);

    void cancelBackupTask(String taskToken);

    /**
     * Get task config for the given task token.
     * This method is only guaranteed to return a result if a task was created but hasn't been started yet.
     * Returned object class is {@link SingleBackupTaskConfig} for tasks created via {@link #createBackupTask(SingleBackupTaskConfig)} and {@link BatchBackupTaskConfig} for tasks created via {@link #createBatchBackupTask(BatchBackupTaskConfig)}
     *
     * @param taskToken token of the task to retrieve config for
     * @return task config or null if token is invalid or the task has been started
     */
    @Nullable
    BackupTaskConfig getTaskConfig(String taskToken);

    void addBackupProgressListener(BackupProgressListener progressListener, Handler progressListenerHandler);

    void removeBackupProgressListener(BackupProgressListener progressListener);

    void addObserver(Observer observer, Handler observerHandler);

    void removeObserver(Observer observer);

    void deleteBackup(Uri backupUri) throws Exception;

    ApkSource createApkSource(Uri backupUri);

    interface Observer {

        void onBackupAdded(String storageId, Backup backup);

        void onBackupRemoved(String storageId, Uri backupUri);

        void onStorageUpdated(String storageId);

    }

    interface BackupProgressListener {

        void onBackupTaskStatusChanged(String storageId, BackupTaskStatus status);

        void onBatchBackupTaskStatusChanged(String storageId, BatchBackupTaskStatus status);

    }

    enum BackupTaskState {
        CREATED, QUEUED, IN_PROGRESS, CANCELLED, SUCCEEDED, FAILED
    }

    class BackupTaskStatus {

        private String mToken;
        private SingleBackupTaskConfig mConfig;
        private BackupTaskState mState;
        private long mCurrentProgress;
        private long mGoal;
        private Backup mBackup;
        private Exception mException;

        private BackupTaskStatus(String token, SingleBackupTaskConfig config, BackupTaskState state, long currentProgress, long goal, @Nullable Backup backup, @Nullable Exception exception) {
            mToken = token;
            mConfig = config;
            mState = state;
            mCurrentProgress = currentProgress;
            mGoal = goal;
            mBackup = backup;
            mException = exception;
        }

        public static BackupTaskStatus created(String token, SingleBackupTaskConfig config) {
            return new BackupTaskStatus(token, config, BackupTaskState.CREATED, 0, 0, null, null);
        }

        public static BackupTaskStatus queued(String token, SingleBackupTaskConfig config) {
            return new BackupTaskStatus(token, config, BackupTaskState.QUEUED, 0, 0, null, null);
        }

        public static BackupTaskStatus inProgress(String token, SingleBackupTaskConfig config, long currentProgress, long goal) {
            return new BackupTaskStatus(token, config, BackupTaskState.IN_PROGRESS, currentProgress, goal, null, null);
        }

        public static BackupTaskStatus cancelled(String token, SingleBackupTaskConfig config) {
            return new BackupTaskStatus(token, config, BackupTaskState.CANCELLED, 0, 0, null, null);
        }

        public static BackupTaskStatus succeeded(String token, SingleBackupTaskConfig config, Backup backup) {
            return new BackupTaskStatus(token, config, BackupTaskState.SUCCEEDED, 0, 0, backup, null);
        }

        public static BackupTaskStatus failed(String token, SingleBackupTaskConfig config, Exception e) {
            return new BackupTaskStatus(token, config, BackupTaskState.FAILED, 0, 0, null, e);
        }

        public String token() {
            return mToken;
        }

        public SingleBackupTaskConfig config() {
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

        public Backup backup() {
            return mBackup;
        }

        public Exception exception() {
            return mException;
        }
    }

    class BatchBackupTaskStatus {

        private String mToken;
        private BackupTaskState mState;

        private SingleBackupTaskConfig mCurrentConfig;

        private int mTotalBackupsCount;
        private int mSucceededBackupsCount;
        private int mFailedBackupsCount;

        private Map<SingleBackupTaskConfig, Backup> mSucceededBackups;
        private Map<SingleBackupTaskConfig, Exception> mFailedBackups;
        private List<SingleBackupTaskConfig> mCancelledBackups;

        private Exception mException;

        private BatchBackupTaskStatus(String token, BackupTaskState state) {
            mToken = token;
            mState = state;
        }

        public static BatchBackupTaskStatus created(String token) {
            return new BatchBackupTaskStatus(token, BackupTaskState.CREATED);
        }

        public static BatchBackupTaskStatus queued(String token) {
            return new BatchBackupTaskStatus(token, BackupTaskState.QUEUED);
        }

        public static BatchBackupTaskStatus inProgress(String token, SingleBackupTaskConfig currentConfig, int totalBackupsCount, int succeededBackupsCount, int failedBackupsCount) {
            BatchBackupTaskStatus status = new BatchBackupTaskStatus(token, BackupTaskState.IN_PROGRESS);
            status.mCurrentConfig = currentConfig;
            status.mTotalBackupsCount = totalBackupsCount;
            status.mSucceededBackupsCount = succeededBackupsCount;
            status.mFailedBackupsCount = failedBackupsCount;

            return status;
        }

        public static BatchBackupTaskStatus cancelled(String token, Map<SingleBackupTaskConfig, Backup> succeededBackups, Map<SingleBackupTaskConfig, Exception> failedBackups, List<SingleBackupTaskConfig> cancelledBackups) {
            BatchBackupTaskStatus status = new BatchBackupTaskStatus(token, BackupTaskState.CANCELLED);
            status.mSucceededBackups = succeededBackups;
            status.mFailedBackups = failedBackups;
            status.mCancelledBackups = cancelledBackups;

            return status;
        }

        public static BatchBackupTaskStatus succeeded(String token, Map<SingleBackupTaskConfig, Backup> succeededBackups, Map<SingleBackupTaskConfig, Exception> failedBackups) {
            BatchBackupTaskStatus status = new BatchBackupTaskStatus(token, BackupTaskState.SUCCEEDED);
            status.mSucceededBackups = succeededBackups;
            status.mFailedBackups = failedBackups;

            return status;
        }

        public static BatchBackupTaskStatus failed(String token, Map<SingleBackupTaskConfig, Backup> succeededBackups, Map<SingleBackupTaskConfig, Exception> failedBackups, List<SingleBackupTaskConfig> remainingBackups, Exception exception) {
            BatchBackupTaskStatus status = new BatchBackupTaskStatus(token, BackupTaskState.FAILED);
            status.mSucceededBackups = succeededBackups;
            status.mFailedBackups = failedBackups;
            status.mCancelledBackups = remainingBackups;
            status.mException = exception;

            return status;
        }

        /**
         * Available in any state
         *
         * @return task token
         */
        public String token() {
            return mToken;
        }

        /**
         * Available in any state
         *
         * @return state of this batch backup task
         */
        public BackupTaskState state() {
            return mState;
        }

        /**
         * Available only in {@link BackupTaskState#IN_PROGRESS} state
         *
         * @return config for the task that is currently being executed
         */
        public SingleBackupTaskConfig currentConfig() {
            return mCurrentConfig;
        }

        /**
         * Available only in {@link BackupTaskState#IN_PROGRESS} state
         *
         * @return total count of backup tasks in this batch backup task
         */
        public int totalBackupsCount() {
            return mTotalBackupsCount;
        }

        /**
         * Available only in {@link BackupTaskState#IN_PROGRESS} state
         *
         * @return number of backup tasks that have succeeded
         */
        public int succeededBackupsCount() {
            return mSucceededBackupsCount;
        }

        /**
         * Available only in {@link BackupTaskState#IN_PROGRESS} state
         *
         * @return number of backup tasks that have failed
         */
        public int failedBackupsCount() {
            return mFailedBackupsCount;
        }

        /**
         * Available only in {@link BackupTaskState#IN_PROGRESS} state
         *
         * @return number of completed backups (succeeded + failed backups)
         */
        public int completedBackupsCount() {
            return succeededBackupsCount() + failedBackupsCount();
        }

        /**
         * Available in {@link BackupTaskState#CANCELLED}, {@link BackupTaskState#SUCCEEDED} and {@link BackupTaskState#FAILED} states
         *
         * @return task config to backup file meta map of successful tasks
         */
        public Map<SingleBackupTaskConfig, Backup> succeededBackups() {
            return mSucceededBackups;
        }

        /**
         * Available in {@link BackupTaskState#CANCELLED}, {@link BackupTaskState#SUCCEEDED} and {@link BackupTaskState#FAILED} states
         *
         * @return task config to exception map of failed tasks
         */
        public Map<SingleBackupTaskConfig, Exception> failedBackups() {
            return mFailedBackups;
        }

        /**
         * Available in {@link BackupTaskState#CANCELLED} and {@link BackupTaskState#FAILED} states
         *
         * @return list of task configs that never got executed because batch backup task was cancelled
         */
        public List<SingleBackupTaskConfig> cancelledBackups() {
            return mCancelledBackups;
        }

        /**
         * Available only in {@link BackupTaskState#FAILED} state
         *
         * @return exception that caused the whole batch backup task to fail
         */
        public Exception exception() {
            return mException;
        }
    }
}
