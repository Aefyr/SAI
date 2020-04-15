package com.aefyr.sai.installerx.resolver.urimess.impl;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import androidx.documentfile.provider.DocumentFile;

import com.aefyr.sai.installerx.resolver.urimess.UriHost;
import com.aefyr.sai.utils.saf.SafUtils;

import java.io.InputStream;

public class AndroidUriHost implements UriHost {

    private Context mContext;

    public AndroidUriHost(Context context) {
        mContext = context;
    }

    @Override
    public String getFileNameFromUri(Uri uri) {
        return SafUtils.getFileNameFromContentUri(mContext, uri);
    }

    @Override
    public long getFileSizeFromUri(Uri uri) {
        DocumentFile documentFile = SafUtils.docFileFromSingleUriOrFileUri(mContext, uri);

        if (documentFile != null)
            return documentFile.length();
        else
            return -1;
    }

    @Override
    public ParcelFileDescriptor openUriAsParcelFd(Uri uri) throws Exception {
        return mContext.getContentResolver().openFileDescriptor(uri, "r");
    }

    @Override
    public InputStream openUriInputStream(Uri uri) throws Exception {
        return mContext.getContentResolver().openInputStream(uri);
    }
}
