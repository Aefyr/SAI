package com.aefyr.sai.model.apksource;

import androidx.annotation.Nullable;

import java.io.InputStream;
import java.util.Set;

/**
 * An ApkSource that can filter out APK files from the backing ZipBackedApkSource
 */
public class FilterApkSource implements ApkSource {

    private ApkSource mWrappedApkSource;
    private Set<String> mFilteredEntries;
    private boolean mBlacklist;

    public FilterApkSource(ApkSource apkSource, Set<String> filteredEntries, boolean blacklist) {
        mWrappedApkSource = apkSource;
        mFilteredEntries = filteredEntries;
        mBlacklist = blacklist;
    }

    @Override
    public boolean nextApk() throws Exception {
        if (!mWrappedApkSource.nextApk())
            return false;

        while (shouldSkip(getApkLocalPath())) {
            if (!mWrappedApkSource.nextApk())
                return false;
        }

        return true;
    }

    private boolean shouldSkip(String localPath) {
        if (mBlacklist)
            return mFilteredEntries.contains(localPath);
        else
            return !mFilteredEntries.contains(localPath);
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
    public String getApkLocalPath() throws Exception {
        return mWrappedApkSource.getApkLocalPath();
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
