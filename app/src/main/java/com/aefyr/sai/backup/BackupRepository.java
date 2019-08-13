package com.aefyr.sai.backup;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aefyr.sai.model.backup.PackageMeta;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class BackupRepository {

    private static BackupRepository sInstance;

    private Context mContext;

    private MutableLiveData<List<PackageMeta>> mPackagesLiveData = new MutableLiveData<>();

    public static BackupRepository getInstance(Context c) {
        synchronized (BackupRepository.class) {
            return sInstance != null ? sInstance : new BackupRepository(c);
        }
    }

    private BackupRepository(Context c) {
        sInstance = this;

        mContext = c.getApplicationContext();
        mPackagesLiveData.setValue(new ArrayList<>());

        fetchPackages();
    }

    public LiveData<List<PackageMeta>> getPackages() {
        return mPackagesLiveData;
    }

    private void fetchPackages() {
        new Thread(() -> {
            PackageManager pm = mContext.getPackageManager();

            List<ApplicationInfo> applicationInfos = pm.getInstalledApplications(0);
            List<PackageMeta> packages = new ArrayList<>();

            for (ApplicationInfo applicationInfo : applicationInfos) {
                PackageMeta packageMeta = new PackageMeta.Builder(applicationInfo.packageName)
                        .setLabel(applicationInfo.loadLabel(pm).toString())
                        .setHasSplits(applicationInfo.splitPublicSourceDirs != null && applicationInfo.splitPublicSourceDirs.length > 0)
                        .setIsSystemApp((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                        .build();

                packages.add(packageMeta);
            }
            Collections.sort(packages, (p1, p2) -> p1.label.compareToIgnoreCase(p2.label));

            mPackagesLiveData.postValue(packages);
        }).start();
    }

}
