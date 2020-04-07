package com.aefyr.sai.model.apksource;

import android.content.Context;

import androidx.annotation.Nullable;

import com.aefyr.sai.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * An ApkSource implementation that copies APK files from the wrapped ApkSource to a temp file. Used to fix unknown APK sizes when necessary
 */
public class CopyToFileApkSource implements ApkSource {

    private Context mContext;
    private ApkSource mWrappedApkSource;

    private File mTempDir;
    private File mCurrentApkFile;

    public CopyToFileApkSource(Context context, ApkSource wrappedApkSource) {
        mContext = context.getApplicationContext();
        mWrappedApkSource = wrappedApkSource;
    }

    @Override
    public boolean nextApk() throws Exception {
        if (!mWrappedApkSource.nextApk())
            return false;

        if (mTempDir == null)
            mTempDir = createTempDir();

        if (mCurrentApkFile != null)
            IOUtils.deleteRecursively(mCurrentApkFile);


        mCurrentApkFile = new File(mTempDir, mWrappedApkSource.getApkName());

        try (InputStream in = mWrappedApkSource.openApkInputStream(); OutputStream out = new FileOutputStream(mCurrentApkFile)) {
            IOUtils.copyStream(in, out);
        }

        return true;
    }

    @Override
    public InputStream openApkInputStream() throws Exception {
        return new FileInputStream(mCurrentApkFile);
    }

    @Override
    public long getApkLength() {
        return mCurrentApkFile.length();
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
        Exception suppressedException = null;
        try {
            mWrappedApkSource.close();
        } catch (Exception e) {
            suppressedException = e;
        }

        if (mTempDir != null) {
            IOUtils.deleteRecursively(mTempDir);
        }

        if (suppressedException != null)
            throw suppressedException;
    }

    @Nullable
    @Override
    public String getAppName() {
        return mWrappedApkSource.getAppName();
    }

    private File createTempDir() {
        File tempDir = new File(mContext.getFilesDir(), "CopyToFileApkSource");
        tempDir = new File(tempDir, String.valueOf(System.currentTimeMillis()));
        tempDir.mkdirs();
        return tempDir;
    }
}
