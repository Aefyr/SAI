package com.aefyr.sai.backup2.impl;

import android.net.Uri;

import com.aefyr.sai.backup2.Backup;
import com.aefyr.sai.backup2.BackupComponent;

import java.util.List;

public class MutableBackup implements Backup {

    public Uri uri;
    public String pkg;
    public String label;
    public long versionCode;
    public String versionName;
    public long exportTimestamp;
    public Uri iconUri;
    public String contentHash;
    public String storageId;
    public List<BackupComponent> components;


    @Override
    public String storageId() {
        return storageId;
    }

    @Override
    public Uri uri() {
        return uri;
    }

    @Override
    public String pkg() {
        return pkg;
    }

    @Override
    public String appName() {
        return label;
    }

    @Override
    public Uri iconUri() {
        return iconUri;
    }

    @Override
    public long versionCode() {
        return versionCode;
    }

    @Override
    public String versionName() {
        return versionName;
    }

    @Override
    public long creationTime() {
        return exportTimestamp;
    }

    @Override
    public String contentHash() {
        return contentHash;
    }

    @Override
    public List<BackupComponent> components() {
        return components;
    }
}
