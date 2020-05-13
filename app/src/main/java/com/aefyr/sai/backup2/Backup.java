package com.aefyr.sai.backup2;

import android.net.Uri;

import com.aefyr.sai.model.common.PackageMeta;

public interface Backup {

    String storageId();

    Uri uri();

    String pkg();

    String appName();

    Uri iconUri();

    long versionCode();

    String versionName();

    long creationTime();

    String contentHash();

    default PackageMeta toPackageMeta() {
        return new PackageMeta.Builder(pkg())
                .setLabel(appName())
                .setVersionCode(versionCode())
                .setVersionName(versionName())
                .setIconUri(iconUri())
                .build();
    }

}
