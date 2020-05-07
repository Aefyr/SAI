package com.aefyr.sai.backup2;

import android.net.Uri;

import com.aefyr.sai.model.common.PackageMeta;

public class BackupFileMeta {

    public Uri uri;
    public String pkg;
    public String label;
    public long versionCode;
    public String versionName;
    public long exportTimestamp;
    public Uri iconUri;
    public String contentHash;
    public String storageId;

    public PackageMeta toPackageMeta() {
        return new PackageMeta.Builder(pkg)
                .setLabel(label)
                .setVersionCode(versionCode)
                .setVersionName(versionName)
                .setIconUri(iconUri)
                .build();
    }

}
