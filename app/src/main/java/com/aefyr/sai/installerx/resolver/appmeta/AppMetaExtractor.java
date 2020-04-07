package com.aefyr.sai.installerx.resolver.appmeta;

import androidx.annotation.NonNull;

import com.aefyr.sai.installerx.resolver.meta.ApkSourceFile;

import java.io.InputStream;

/**
 * A class that helps to extract some metadata about the app in a ZIP archive (.apks/.xapk/etc)
 */
public interface AppMetaExtractor {

    boolean wantEntry(ApkSourceFile.Entry entry);

    void consumeEntry(ApkSourceFile.Entry entry, InputStream entryInputStream);

    @NonNull
    AppMeta buildMeta();
}
