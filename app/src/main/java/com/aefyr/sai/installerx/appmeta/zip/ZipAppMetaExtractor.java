package com.aefyr.sai.installerx.appmeta.zip;

import androidx.annotation.NonNull;

import com.aefyr.sai.installerx.appmeta.AppMeta;

import java.io.InputStream;
import java.util.zip.ZipEntry;

/**
 * A class that helps to extract some metadata about the app in a ZIP archive (.apks/.xapk/etc)
 */
public interface ZipAppMetaExtractor {

    boolean wantEntry(ZipEntry entry);

    void consumeEntry(ZipEntry entry, InputStream entryInputStream);

    @NonNull
    AppMeta buildMeta();
}
