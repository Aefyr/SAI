package com.aefyr.sai.model.apksource;

import androidx.annotation.Nullable;

import java.io.InputStream;

public interface ApkSource extends AutoCloseable {

    boolean nextApk() throws Exception;

    InputStream openApkInputStream() throws Exception;

    long getApkLength() throws Exception;

    String getApkName() throws Exception;

    String getApkLocalPath() throws Exception;

    @Override
    default void close() throws Exception {

    }

    /**
     * Returns the name of the app this ApkSource will install or null if unknown
     *
     * @return name of the app this ApkSource will install or null if unknown
     */
    @Nullable
    default String getAppName() {
        return null;
    }
}
