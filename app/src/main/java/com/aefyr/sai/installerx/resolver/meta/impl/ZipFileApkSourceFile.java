package com.aefyr.sai.installerx.resolver.meta.impl;

import com.aefyr.sai.installerx.resolver.meta.ApkSourceFile;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Utils;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ZipFileApkSourceFile implements ApkSourceFile {


    private File mFile;
    private String mName;

    private ZipFile mZipFile;

    public ZipFileApkSourceFile(File zipFile, String originalFileName) {
        mFile = zipFile;
        mName = originalFileName;
    }

    @Override
    public List<Entry> listEntries() throws Exception {
        if (mZipFile == null) {
            mZipFile = new ZipFile(mFile);
        }

        List<Entry> entries = new ArrayList<>();
        Enumeration<? extends ZipEntry> zipEntries = mZipFile.entries();

        while (zipEntries.hasMoreElements()) {
            ZipEntry zipEntry = zipEntries.nextElement();
            entries.add(new InternalEntry(zipEntry, Utils.getFileNameFromZipEntry(zipEntry), zipEntry.getName(), zipEntry.getSize()));
        }

        return entries;
    }

    @Override
    public InputStream openEntryInputStream(Entry entry) throws Exception {
        return mZipFile.getInputStream(((InternalEntry) entry).mZipEntry);
    }

    @Override
    public String getName() {
        return mName;
    }

    @Override
    public void close() {
        IOUtils.closeSilently(mZipFile);
    }

    private static class InternalEntry extends Entry {

        private ZipEntry mZipEntry;

        private InternalEntry(ZipEntry zipEntry, String name, String localPath, long size) {
            super(name, localPath, size);
            mZipEntry = zipEntry;
        }
    }
}
