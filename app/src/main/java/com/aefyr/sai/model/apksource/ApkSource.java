package com.aefyr.sai.model.apksource;

import java.io.InputStream;

public interface ApkSource {

    boolean nextApk() throws Exception;

    InputStream openApkInputStream() throws Exception;

    long getApkLength() throws Exception;

    String getApkName() throws Exception;
}
