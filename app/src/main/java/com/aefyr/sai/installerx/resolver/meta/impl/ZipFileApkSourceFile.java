package com.aefyr.sai.installerx.resolver.meta.impl;

import androidx.annotation.Nullable;

import com.aefyr.sai.installerx.resolver.meta.ApkSourceFile;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Utils;

import java.io.File;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipFileApkSourceFile implements ApkSourceFile {


    private File mFile;
    private String mName;

    private ZipFile mZipFile;
    private Enumeration<? extends ZipEntry> mEntries;
    private ZipEntry mCurrentEntry;

    public ZipFileApkSourceFile(File zipFile, String originalFileName) {
        mFile = zipFile;
        mName = originalFileName;
    }

    @Nullable
    @Override
    public Entry nextEntry() throws Exception {
        if (mZipFile == null) {
            mZipFile = new ZipFile(mFile);
            mEntries = mZipFile.entries();
        }

        if (!mEntries.hasMoreElements())
            return null;

        mCurrentEntry = mEntries.nextElement();

        return new Entry(Utils.getFileNameFromZipEntry(mCurrentEntry), mCurrentEntry.getName());
    }

    @Override
    public InputStream openEntryInputStream() throws Exception {
        return mZipFile.getInputStream(mCurrentEntry);
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public void close() {
        IOUtils.closeSilently(mZipFile);
    }
}
