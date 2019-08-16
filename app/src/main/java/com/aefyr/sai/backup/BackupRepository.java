package com.aefyr.sai.backup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aefyr.sai.model.backup.PackageMeta;
import com.aefyr.sai.utils.Logs;
import com.aefyr.sai.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BackupRepository {
    private static final String TAG = "BackupRepository";

    private static BackupRepository sInstance;

    private Context mContext;
    private Executor mExecutor = Executors.newSingleThreadExecutor();

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

        IntentFilter packagesStuffIntentFilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        packagesStuffIntentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        packagesStuffIntentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packagesStuffIntentFilter.addDataScheme("package");
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                //TODO can be handled without re-fetching all packages
                fetchPackages();
            }
        }, packagesStuffIntentFilter);

        fetchPackages();
    }

    public LiveData<List<PackageMeta>> getPackages() {
        return mPackagesLiveData;
    }

    private void fetchPackages() {
        mExecutor.execute(() -> {
            long start = System.currentTimeMillis();

            PackageManager pm = mContext.getPackageManager();

            List<ApplicationInfo> applicationInfos = pm.getInstalledApplications(0);
            List<PackageMeta> packages = new ArrayList<>();

            for (ApplicationInfo applicationInfo : applicationInfos) {
                PackageInfo packageInfo;
                try {
                    packageInfo = pm.getPackageInfo(applicationInfo.packageName, 0);
                } catch (Exception e) {
                    Logs.wtf(TAG, e);
                    Logs.logException(e);
                    continue;
                }


                PackageMeta packageMeta = new PackageMeta.Builder(applicationInfo.packageName)
                        .setLabel(applicationInfo.loadLabel(pm).toString())
                        .setHasSplits(applicationInfo.splitPublicSourceDirs != null && applicationInfo.splitPublicSourceDirs.length > 0)
                        .setIsSystemApp((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                        .serVersionCode(Utils.apiIsAtLeast(Build.VERSION_CODES.P) ? packageInfo.getLongVersionCode() : packageInfo.versionCode)
                        .setVersionName(packageInfo.versionName)
                        .build();

                packages.add(packageMeta);
            }
            Collections.sort(packages, (p1, p2) -> p1.label.compareToIgnoreCase(p2.label));

            Log.d(TAG, String.format("Loaded packages in %d ms", (System.currentTimeMillis() - start)));

            mPackagesLiveData.postValue(packages);
        });
    }

}
