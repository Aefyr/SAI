package com.aefyr.sai.installerx.resolver.meta.impl;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.aefyr.sai.installerx.resolver.meta.ApkSourceFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class InstalledAppApkSourceFile implements ApkSourceFile {
    public static final String FAKE_EXTENSION = "installedApp";

    private Context mContext;
    private String mPackage;

    private List<File> mApkFiles;

    public InstalledAppApkSourceFile(Context context, String pkg) {
        mContext = context.getApplicationContext();
        mPackage = pkg;
    }

    @Override
    public List<Entry> listEntries() throws Exception {
        if (mApkFiles == null) {
            mApkFiles = new ArrayList<>();

            PackageManager pm = mContext.getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(mPackage, 0);

            mApkFiles.add(new File(appInfo.publicSourceDir));

            if (appInfo.splitPublicSourceDirs != null) {
                for (String splitPath : appInfo.splitPublicSourceDirs) {
                    mApkFiles.add(new File(splitPath));
                }
            }
        }

        ArrayList<Entry> entries = new ArrayList<>();
        for (File apkFile : mApkFiles) {
            entries.add(new Entry(apkFile.getName(), apkFile.getAbsolutePath(), apkFile.length()));
        }

        return entries;
    }

    @Override
    public InputStream openEntryInputStream(Entry entry) throws Exception {
        return new FileInputStream(entry.getLocalPath());
    }

    @Override
    public String getName() {
        return mPackage + "." + FAKE_EXTENSION;
    }

}
