package com.aefyr.sai.installer;

import android.content.Context;

import com.aefyr.sai.R;
import com.aefyr.sai.utils.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

class QueuedInstallation {
    private Context mContext;
    private File mZipWithApkFiles;
    private List<File> mApkFiles;
    private File mCacheDirectory;
    private long mId;

    QueuedInstallation(Context c, List<File> apkFiles, long id) {
        mContext = c;
        mApkFiles = apkFiles;
        mId = id;
    }

    QueuedInstallation(Context c, File zipWithApkFiles, long id) {
        mContext = c;
        mZipWithApkFiles = zipWithApkFiles;
        mId = id;
    }

    long getId() {
        return mId;
    }

    List<File> getApkFiles() throws Exception {
        if (mApkFiles != null)
            return mApkFiles;

        extractZip();
        return mApkFiles;
    }

    void clear() {
        if (mCacheDirectory != null) {
            deleteFile(mCacheDirectory);
        }
    }

    private void deleteFile(File f) {
        if (f.isDirectory()) {
            for (File child : f.listFiles())
                deleteFile(child);
        }
        f.delete();
    }

    private void extractZip() throws Exception {
        if (mZipWithApkFiles == null)
            return;

        mCacheDirectory = new File(mContext.getCacheDir(), String.valueOf(System.currentTimeMillis()));
        mCacheDirectory.mkdirs();

        ZipFile zipFile = new ZipFile(mZipWithApkFiles);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();
        mApkFiles = new ArrayList<>(zipFile.size());

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if (entry.isDirectory() || !entry.getName().endsWith(".apk"))
                throw new IllegalArgumentException(mContext.getString(R.string.installer_error_zip_contains_non_apks));


            File tempApkFile = new File(mCacheDirectory, entry.getName());

            FileOutputStream outputStream = new FileOutputStream(tempApkFile);
            InputStream inputStream = zipFile.getInputStream(entry);
            IOUtils.copyStream(inputStream, outputStream);

            outputStream.close();
            inputStream.close();

            mApkFiles.add(tempApkFile);
        }
        zipFile.close();
    }
}
