package com.aefyr.sai.model.apksource;

import java.util.zip.ZipEntry;

/**
 * An ApkSource backed by a zip archive
 */
public interface ZipBackedApkSource extends ApkSource {

    /**
     * @return ZipEntry for the current APK
     */
    ZipEntry getEntry();

}
