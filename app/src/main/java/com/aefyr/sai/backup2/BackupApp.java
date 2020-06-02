package com.aefyr.sai.backup2;

import com.aefyr.sai.model.common.PackageMeta;

public interface BackupApp {

    PackageMeta packageMeta();

    boolean isInstalled();

    BackupStatus backupStatus();
}
