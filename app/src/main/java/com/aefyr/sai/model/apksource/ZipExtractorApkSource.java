package com.aefyr.sai.model.apksource;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.aefyr.sai.R;
import com.aefyr.sai.model.filedescriptor.FileDescriptor;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * @deprecated Use {@link ZipApkSource} wrapped in a {@link CopyToFileApkSource} instead
 */
@Deprecated
public class ZipExtractorApkSource implements ApkSource {
    private Context mContext;
    private FileDescriptor mZipFileDescriptor;
    private boolean mIsOpen;
    private int mSeenApkFiles = 0;

    private ZipInputStream mZipInputStream;
    private ZipEntry mCurrentZipEntry;
    private File mExtractedFilesDir;
    private File mCurrentExtractedZipEntryFile;

    public ZipExtractorApkSource(Context c, FileDescriptor zipFileDescriptor) {
        mContext = c;
        mZipFileDescriptor = zipFileDescriptor;

        File extractedApksDir = new File(c.getFilesDir(), "extractedApks");
        extractedApksDir.mkdirs();
        mExtractedFilesDir = new File(extractedApksDir, String.valueOf(System.currentTimeMillis()));
        mExtractedFilesDir.mkdirs();
    }

    @Override
    public boolean nextApk() throws Exception {
        if (!mIsOpen) {
            mZipInputStream = new ZipInputStream(mZipFileDescriptor.open());
            mIsOpen = true;
        }

        do {
            mCurrentZipEntry = mZipInputStream.getNextEntry();
        } while (mCurrentZipEntry != null && (mCurrentZipEntry.isDirectory() || !mCurrentZipEntry.getName().endsWith(".apk")));

        if (mCurrentZipEntry == null) {
            mZipInputStream.close();

            if (mSeenApkFiles == 0)
                throw new IllegalArgumentException(mContext.getString(R.string.installer_error_zip_contains_no_apks));

            return false;
        }
        mSeenApkFiles++;

        extractCurrentEntry();

        return true;
    }

    @Override
    public InputStream openApkInputStream() throws Exception {
        return new FileInputStream(mCurrentExtractedZipEntryFile);
    }

    @Override
    public long getApkLength() {
        return mCurrentExtractedZipEntryFile.length();
    }

    @Override
    public String getApkName() {
        return mCurrentExtractedZipEntryFile.getName();
    }

    @Override
    public void close() {
        IOUtils.deleteRecursively(mExtractedFilesDir);
    }

    @Nullable
    @Override
    public String getAppName() {
        try {
            return mZipFileDescriptor.name();
        } catch (Exception e) {
            Log.w("ZipExtractorApkSource", "Unable to get app name", e);
            return null;
        }
    }

    private void extractCurrentEntry() throws Exception {
        mCurrentExtractedZipEntryFile = new File(mExtractedFilesDir, Utils.getFileNameFromZipEntry(mCurrentZipEntry));
        try (FileOutputStream fileOutputStream = new FileOutputStream(mCurrentExtractedZipEntryFile)) {
            IOUtils.copyStream(mZipInputStream, fileOutputStream);
        }
    }
}
