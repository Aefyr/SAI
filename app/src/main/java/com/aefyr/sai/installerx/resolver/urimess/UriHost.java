package com.aefyr.sai.installerx.resolver.urimess;

import android.net.Uri;
import android.os.ParcelFileDescriptor;

public interface UriHost {

    String getFileNameFromUri(Uri uri);

    ParcelFileDescriptor openUriAsParcelFd(Uri uri) throws Exception;

}
