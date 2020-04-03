package com.aefyr.sai.model.apksource;

import androidx.annotation.Nullable;

import java.io.InputStream;
import java.util.Set;
import java.util.zip.ZipEntry;

/**
 * An ApkSource that can filter out APK files from the backing ZipBackedApkSource
 */
public class ZipFilterApkSource implements ZipBackedApkSource {

    private ZipBackedApkSource mWrappedApkSource;
    private Set<String> mFilteredEntries;
    private boolean mBlacklist;

    public ZipFilterApkSource(ZipBackedApkSource apkSource, Set<String> filteredEntries, boolean blacklist) {
        mWrappedApkSource = apkSource;
        mFilteredEntries = filteredEntries;
        mBlacklist = blacklist;
    }

    @Override
    public ZipEntry getEntry() {
        return mWrappedApkSource.getEntry();
    }

    @Override
    public boolean nextApk() throws Exception {
        if (!mWrappedApkSource.nextApk())
            return false;

        while (shouldSkip(getEntry())) {
            if (!mWrappedApkSource.nextApk())
                return false;
        }

        return true;
    }

    private boolean shouldSkip(ZipEntry entry) {
        if (mBlacklist)
            return mFilteredEntries.contains(entry.getName());
        else
            return !mFilteredEntries.contains(entry.getName());
    }

    @Override
    public InputStream openApkInputStream() throws Exception {
        return mWrappedApkSource.openApkInputStream();
    }

    @Override
    public long getApkLength() throws Exception {
        return mWrappedApkSource.getApkLength();
    }

    @Override
    public String getApkName() throws Exception {
        return mWrappedApkSource.getApkName();
    }

    @Override
    public void close() throws Exception {
        mWrappedApkSource.close();
    }

    @Nullable
    @Override
    public String getAppName() {
        return mWrappedApkSource.getAppName();
    }
}
