package com.aefyr.sai.utils;

import com.aefyr.sai.model.backup.PackageMeta;

public class BackupNameFormat {

    public static final String ARG_NAME = "NAME";
    public static final String ARG_VERSION = "VERSION";
    public static final String ARG_VERSION_CODE = "CODE";
    public static final String ARG_PACKAGE = "PACKAGE";
    public static final String ARG_TIMESTAMP = "TIMESTAMP";

    public static String format(String format, PackageMeta packageMeta) {
        return format.replace(ARG_NAME, packageMeta.label)
                .replace(ARG_VERSION_CODE, String.valueOf(packageMeta.versionCode))
                .replace(ARG_VERSION, packageMeta.versionName)
                .replace(ARG_PACKAGE, packageMeta.packageName)
                .replace(ARG_TIMESTAMP, String.valueOf(System.currentTimeMillis() / 1000));
    }

}
