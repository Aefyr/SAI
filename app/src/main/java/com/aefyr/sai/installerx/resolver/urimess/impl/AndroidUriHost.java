package com.aefyr.sai.installerx.resolver.urimess.impl;

import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

import com.aefyr.sai.installerx.resolver.urimess.UriHost;
import com.aefyr.sai.utils.saf.SafUtils;

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
    public ParcelFileDescriptor openUriAsParcelFd(Uri uri) throws Exception {
        return mContext.getContentResolver().openFileDescriptor(uri, "r");
    }
}
