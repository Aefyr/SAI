package com.aefyr.sai.installerx.resolver.urimess.impl;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;

import androidx.documentfile.provider.DocumentFile;

import com.aefyr.sai.R;
import com.aefyr.sai.installerx.resolver.urimess.UriHost;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Logs;
import com.aefyr.sai.utils.Utils;
import com.aefyr.sai.utils.saf.SafUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Objects;

public class AndroidUriHost implements UriHost {
    private static final long MAX_FILE_LENGTH_FOR_COPY = 1024 * 1024 * 100;

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
    public UriAsFile openUriAsFile(Uri uri) throws Exception {
        try {
            return new ProcSelfFdUriAsFile(uri);
        } catch (Exception e) {
            boolean hasReadExternalStoragePermission = true;
            if (Utils.apiIsAtLeast(Build.VERSION_CODES.M)) {
                hasReadExternalStoragePermission = mContext.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            }

            Logs.logException(new IOException(String.format("Unable to use /proc/self/fd, READ_EXTERNAL_STORAGE permission = %s", hasReadExternalStoragePermission)));
            return new CopyFileUriAsFile(uri, MAX_FILE_LENGTH_FOR_COPY);
        }
    }


    @Override
    public InputStream openUriInputStream(Uri uri) throws Exception {
        return mContext.getContentResolver().openInputStream(uri);
    }

    private class ProcSelfFdUriAsFile implements UriAsFile {

        private ParcelFileDescriptor mFd;

        private ProcSelfFdUriAsFile(Uri uri) throws Exception {
            mFd = mContext.getContentResolver().openFileDescriptor(uri, "r");
            if (!file().canRead())
                throw new IOException("Can't read /proc/self/fd/" + mFd.getFd());
        }

        @Override
        public File file() {
            return SafUtils.parcelFdToFile(mFd);
        }

        @Override
        public void close() throws Exception {
            if (mFd != null)
                mFd.close();
        }
    }

    private class CopyFileUriAsFile implements UriAsFile {

        private File mTempFile;

        private CopyFileUriAsFile(Uri uri, long maxFileLength) throws Exception {
            if (SafUtils.getFileLengthFromContentUri(mContext, uri) > maxFileLength) {
                throw new IOException(mContext.getString(R.string.installerx_android_uri_host_file_too_big));
            }

            mTempFile = Utils.createTempFileInCache(mContext, "AndroidUriHost.CopyFileUriAsFile", "tmp");
            try (InputStream in = Objects.requireNonNull(mContext.getContentResolver().openInputStream(uri)); OutputStream out = new FileOutputStream(mTempFile)) {
                IOUtils.copyStream(in, out);
            }
        }

        @Override
        public File file() {
            return mTempFile;
        }

        @Override
        public void close() throws Exception {
            mTempFile.delete();
        }
    }
}
