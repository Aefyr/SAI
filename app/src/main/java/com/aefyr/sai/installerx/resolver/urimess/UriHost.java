package com.aefyr.sai.installerx.resolver.urimess;

import android.net.Uri;
import android.os.ParcelFileDescriptor;

import java.io.InputStream;

public interface UriHost {

    String getFileNameFromUri(Uri uri);

    long getFileSizeFromUri(Uri uri);

    ParcelFileDescriptor openUriAsParcelFd(Uri uri) throws Exception;

    InputStream openUriInputStream(Uri uri) throws Exception;

}
