package com.aefyr.sai.backup2.backuptask.config;

import java.util.List;

public class BatchBackupTaskConfig implements BackupTaskConfig {

    private List<SingleBackupTaskConfig> mConfigs;

    public BatchBackupTaskConfig(List<SingleBackupTaskConfig> singleTaskConfigs) {
        mConfigs = singleTaskConfigs;
    }

    public List<SingleBackupTaskConfig> configs() {
        return mConfigs;
    }

}
