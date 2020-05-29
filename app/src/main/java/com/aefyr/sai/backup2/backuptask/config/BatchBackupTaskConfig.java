package com.aefyr.sai.backup2.backuptask.config;

import java.util.List;

public class BatchBackupTaskConfig implements BackupTaskConfig {

    private String mBackupStorageId;
    private List<SingleBackupTaskConfig> mConfigs;

    /**
     * @param backupStorageId   backup storage id for this backup task. Please note that backup storage ids in {@code singleTaskConfigs} will be ignored for batch tasks
     * @param singleTaskConfigs
     */
    public BatchBackupTaskConfig(String backupStorageId, List<SingleBackupTaskConfig> singleTaskConfigs) {
        mBackupStorageId = backupStorageId;
        mConfigs = singleTaskConfigs;
    }

    public List<SingleBackupTaskConfig> configs() {
        return mConfigs;
    }

    @Override
    public String getBackupStorageId() {
        return mBackupStorageId;
    }
}
