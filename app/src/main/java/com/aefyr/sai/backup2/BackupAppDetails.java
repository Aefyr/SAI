package com.aefyr.sai.backup2;

import java.util.List;

public interface BackupAppDetails {

    State state();

    BackupApp app();

    List<BackupFileMeta> backups();

    enum State {
        LOADING, READY, ERROR
    }

}
