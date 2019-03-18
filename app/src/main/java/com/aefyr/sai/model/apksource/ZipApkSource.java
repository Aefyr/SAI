package com.aefyr.sai.model.apksource;

import android.content.Context;

import com.aefyr.sai.R;
import com.aefyr.sai.model.filedescriptor.FileDescriptor;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ZipApkSource implements ApkSource {

    private Context mContext;
    private FileDescriptor mZipFileDescriptor;
    private boolean mIsOpen;

    private ZipInputStream mZipInputStream;
    private ZipEntry mCurrentZipEntry;

    private ZipInputStreamWrapper mWrappedStream;

    public ZipApkSource(Context c, FileDescriptor zipFileDescriptor) {
        mContext = c;
        mZipFileDescriptor = zipFileDescriptor;
    }

    @Override
    public boolean nextApk() throws Exception {
        if (!mIsOpen) {
            mZipInputStream = new ZipInputStream(mZipFileDescriptor.open());
            mWrappedStream = new ZipInputStreamWrapper(mZipInputStream);
            mIsOpen = true;
        }

        mCurrentZipEntry = mZipInputStream.getNextEntry();
        if (mCurrentZipEntry == null)
            return false;

        if (mCurrentZipEntry.isDirectory() || !mCurrentZipEntry.getName().endsWith(".apk"))
            throw new IllegalArgumentException(mContext.getString(R.string.installer_error_zip_contains_non_apks));

        return true;
    }

    @Override
    public InputStream openApkInputStream() {
        return mWrappedStream;
    }

    @Override
    public long getApkLength() {
        return mCurrentZipEntry.getSize();
    }

    @Override
    public String getApkName() {
        return mCurrentZipEntry.getName();
    }

    /**
     * Wraps ZipInputStream so it can be used as seemingly multiple InputStreams that represent each file in the archive.
     * Basically just calls closeEntry instead of close, so ZipInputStream itself won't be closed
     **/
    private static class ZipInputStreamWrapper extends InputStream {

        private ZipInputStream mWrappedStream;

        private ZipInputStreamWrapper(ZipInputStream inputStream) {
            mWrappedStream = inputStream;
        }

        @Override
        public int available() throws IOException {
            return mWrappedStream.available();
        }

        @Override
        public int read() throws IOException {
            return mWrappedStream.read();
        }

        @Override
        public int read(byte[] b) throws IOException {
            return mWrappedStream.read(b);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return mWrappedStream.read(b, off, len);
        }

        @Override
        public void close() throws IOException {
            mWrappedStream.closeEntry();
        }
    }
}
