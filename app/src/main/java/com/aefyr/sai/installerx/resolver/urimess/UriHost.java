package com.aefyr.sai.installerx.resolver.urimess;

import android.net.Uri;

import java.io.File;
import java.io.InputStream;

public interface UriHost {

    String getFileNameFromUri(Uri uri);

    long getFileSizeFromUri(Uri uri);

    UriAsFile openUriAsFile(Uri uri) throws Exception;

    InputStream openUriInputStream(Uri uri) throws Exception;

    interface UriAsFile extends AutoCloseable {

        File file();

    }

}
