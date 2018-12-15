package com.aefyr.sai.installer;

import java.io.File;
import java.util.List;

class QueuedInstallation {
    List<File> apkFiles;
    long id;

    QueuedInstallation(List<File> apkFiles, long id) {
        this.apkFiles = apkFiles;
        this.id = id;
    }
}
