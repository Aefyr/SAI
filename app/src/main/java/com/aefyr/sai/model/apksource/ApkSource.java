package com.aefyr.sai.model.apksource;

import java.io.InputStream;

public interface ApkSource extends AutoCloseable {

    boolean nextApk() throws Exception;

    InputStream openApkInputStream() throws Exception;

    long getApkLength() throws Exception;

    String getApkName() throws Exception;

    @Override
    default void close() throws Exception {

    }
}
