package com.aefyr.sai.backup2;

import androidx.lifecycle.LiveData;

import com.aefyr.sai.model.common.PackageMeta;

import java.util.List;

public interface BackupManager {

    LiveData<List<PackageMeta>> getInstalledPackages();

    LiveData<List<BackupApp>> getApps();

    void enqueueBackup(BackupTaskConfig backupTaskConfig);

    void reindex();

    LiveData<IndexingStatus> getIndexingStatus();

    class IndexingStatus {
        private boolean mInProgress;
        private int mProgress;
        private int mGoal;

        public IndexingStatus(int progress, int goal) {
            mInProgress = true;
            mProgress = progress;
            mGoal = goal;
        }

        public IndexingStatus() {
            mInProgress = false;
        }

        public boolean isInProgress() {
            return mInProgress;
        }

        public int progress() {
            return mProgress;
        }

        public int goal() {
            return mGoal;
        }


    }

}
