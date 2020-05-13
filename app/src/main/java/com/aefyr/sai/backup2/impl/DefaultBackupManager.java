package com.aefyr.sai.backup2.impl;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.aefyr.sai.backup2.Backup;
import com.aefyr.sai.backup2.BackupApp;
import com.aefyr.sai.backup2.BackupAppDetails;
import com.aefyr.sai.backup2.BackupIndex;
import com.aefyr.sai.backup2.BackupManager;
import com.aefyr.sai.backup2.BackupStatus;
import com.aefyr.sai.backup2.BackupStorage;
import com.aefyr.sai.backup2.backuptask.config.BatchBackupTaskConfig;
import com.aefyr.sai.backup2.backuptask.config.SingleBackupTaskConfig;
import com.aefyr.sai.backup2.impl.storage.LocalBackupStorage;
import com.aefyr.sai.model.common.PackageMeta;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.utils.Stopwatch;
import com.aefyr.sai.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultBackupManager implements BackupManager, BackupStorage.Observer {
    private static final String TAG = "DefaultBackupManager";

    private static DefaultBackupManager sInstance;

    private Context mContext;
    private BackupStorage mStorage;
    private BackupIndex mIndex;
    private PreferencesHelper mPrefsHelper;

    private Map<String, PackageMeta> mInstalledApps;
    private MutableLiveData<List<PackageMeta>> mInstalledAppsLiveData = new MutableLiveData<>(Collections.emptyList());
    private Handler mWorkerHandler;

    private Map<String, BackupApp> mApps;
    private MutableLiveData<List<BackupApp>> mAppsLiveData = new MutableLiveData<>(Collections.emptyList());

    private MutableLiveData<IndexingStatus> mIndexingStatus = new MutableLiveData<>(new IndexingStatus());

    private final Set<AppsObserver> mAppsObservers = new HashSet<>();

    private ExecutorService mMiscExecutor = Executors.newCachedThreadPool();

    public static synchronized DefaultBackupManager getInstance(Context context) {
        return sInstance != null ? sInstance : new DefaultBackupManager(context);
    }

    private DefaultBackupManager(Context context) {
        mContext = context.getApplicationContext();
        mStorage = LocalBackupStorage.getInstance(context);
        mIndex = DaoBackedBackupIndex.getInstance(context);
        mPrefsHelper = PreferencesHelper.getInstance(mContext);

        HandlerThread workerThread = new HandlerThread("DefaultBackupManager Worker Thread");
        workerThread.start();
        mWorkerHandler = new Handler(workerThread.getLooper());

        mStorage.addObserver(this, mWorkerHandler);

        IntentFilter packagesStuffIntentFilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        packagesStuffIntentFilter.addAction(Intent.ACTION_PACKAGE_CHANGED);
        packagesStuffIntentFilter.addAction(Intent.ACTION_PACKAGE_REMOVED);
        packagesStuffIntentFilter.addDataScheme("package");
        mContext.registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                updateAppInAppList(Objects.requireNonNull(intent.getData()).getSchemeSpecificPart());
            }
        }, packagesStuffIntentFilter, null, mWorkerHandler);

        mWorkerHandler.post(this::fetchPackages);

        if (!mPrefsHelper.isInitialIndexingDone()) {
            mWorkerHandler.post(this::scanBackups);
        }


        sInstance = this;
    }

    @Override
    public LiveData<List<PackageMeta>> getInstalledPackages() {
        return mInstalledAppsLiveData;
    }

    @Override
    public LiveData<List<BackupApp>> getApps() {
        return mAppsLiveData;
    }

    @Override
    public void enqueueBackup(SingleBackupTaskConfig config) {
        BackupService2.enqueueBackup(mContext, mStorage.createBackupTask(config));
    }

    @Override
    public void enqueueBackup(BatchBackupTaskConfig config) {
        BackupService2.enqueueBackup(mContext, mStorage.createBatchBackupTask(config));
    }

    @Override
    public void reindex() {
        mWorkerHandler.post(this::scanBackups);
    }

    @Override
    public LiveData<IndexingStatus> getIndexingStatus() {
        return mIndexingStatus;
    }

    @Override
    public LiveData<BackupAppDetails> getAppDetails(String pkg) {
        return new LiveAppDetails(pkg);
    }

    @Override
    public void deleteBackup(String storageId, Uri backupUri, @Nullable BackupDeletionCallback callback, @Nullable Handler callbackHandler) {
        mMiscExecutor.execute(() -> {
            try {
                mStorage.deleteBackup(backupUri);

                if (callback != null && callbackHandler != null)
                    callbackHandler.post(() -> callback.onBackupDeleted(storageId, backupUri));

            } catch (Exception e) {
                Log.w(TAG, "Unable to delete backup", e);

                if (callback != null && callbackHandler != null)
                    callbackHandler.post(() -> callback.onFailedToDeleteBackup(storageId, backupUri, e));
            }
        });
    }

    @WorkerThread
    @Nullable
    private PackageMeta getInstalledApp(String pkg) {
        enforceWorkerThread();
        return mInstalledApps.get(pkg);
    }

    @WorkerThread
    @Nullable
    private BackupApp getApp(String pkg) {
        enforceWorkerThread();
        return mApps.get(pkg);
    }

    private void addAppsObserver(AppsObserver observer) {
        synchronized (mAppsObservers) {
            mAppsObservers.add(observer);
        }
    }

    private void removeAppsObserver(AppsObserver observer) {
        synchronized (mAppsObservers) {
            mAppsObservers.remove(observer);
        }
    }

    private void notifyInstalledAppInvalidated(String pkg) {
        synchronized (mAppsObservers) {
            for (AppsObserver observer : mAppsObservers)
                observer.onInstalledAppInvalidated(pkg);
        }
    }

    private void notifyAppInvalidated(String pkg) {
        synchronized (mAppsObservers) {
            for (AppsObserver observer : mAppsObservers)
                observer.onAppInvalidated(pkg);
        }
    }


    @WorkerThread
    private void fetchPackages() {
        enforceWorkerThread();

        long start = System.currentTimeMillis();

        PackageManager pm = mContext.getPackageManager();

        List<ApplicationInfo> applicationInfos = pm.getInstalledApplications(0);
        List<PackageInfo> packageInfos = pm.getInstalledPackages(0);

        HashMap<String, PackageInfo> packageInfoIndex = new HashMap<>(packageInfos.size());
        for (PackageInfo packageInfo : packageInfos)
            packageInfoIndex.put(packageInfo.packageName, packageInfo);

        Map<String, PackageMeta> packages = new HashMap<>();

        for (ApplicationInfo applicationInfo : applicationInfos) {
            PackageInfo packageInfo = packageInfoIndex.get(applicationInfo.packageName);
            if (packageInfo == null) {
                Log.wtf(TAG, String.format("PackageInfo is null for %s", applicationInfo.packageName));
                continue;
            }


            PackageMeta packageMeta = new PackageMeta.Builder(applicationInfo.packageName)
                    .setLabel(applicationInfo.loadLabel(pm).toString())
                    .setHasSplits(applicationInfo.splitPublicSourceDirs != null && applicationInfo.splitPublicSourceDirs.length > 0)
                    .setIsSystemApp((applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                    .setVersionCode(Utils.apiIsAtLeast(Build.VERSION_CODES.P) ? packageInfo.getLongVersionCode() : packageInfo.versionCode)
                    .setVersionName(packageInfo.versionName)
                    .setIcon(applicationInfo.icon)
                    .setInstallTime(packageInfo.firstInstallTime)
                    .setUpdateTime(packageInfo.lastUpdateTime)
                    .build();

            packages.put(packageMeta.packageName, packageMeta);
        }

        Log.d(TAG, String.format("Loaded packages in %d ms", (System.currentTimeMillis() - start)));

        mInstalledApps = packages;
        mInstalledAppsLiveData.postValue(new ArrayList<>(mInstalledApps.values()));

        rebuildAppList();
    }

    @WorkerThread
    private void scanBackups() {
        enforceWorkerThread();

        mIndexingStatus.postValue(new IndexingStatus(0, 1));
        try {
            Stopwatch sw = new Stopwatch();
            Log.i(TAG, "Indexing backup storage...");

            List<Backup> backups = new ArrayList<>();

            List<Uri> backupFileUris = mStorage.listBackupFiles();
            for (int i = 0; i < backupFileUris.size(); i++) {
                Uri backupFileUri = backupFileUris.get(i);
                String fileHash = mStorage.getBackupFileHash(backupFileUri);

                Log.i(TAG, String.format("Indexing %s@%s", backupFileUri, fileHash));

                try {
                    Backup backup = mStorage.getBackupByUri(backupFileUri);
                    backups.add(backup);
                    Log.i(TAG, String.format("Indexed %s@%s", backupFileUri, fileHash));
                } catch (Exception e) {
                    Log.w(TAG, String.format("Unable to get meta for %s@%s, skipping", backupFileUri, fileHash), e);
                }

                mIndexingStatus.postValue(new IndexingStatus(i + 1, backupFileUris.size()));
            }

            try {
                mIndex.rewrite(backups);
                Log.i(TAG, "Index rewritten");
            } catch (Exception e) {
                Log.w(TAG, "Unable to rewrite index", e);
                throw e;
            }

            mPrefsHelper.setInitialIndexingDone(true);
            Log.i(TAG, String.format("Backup storage indexed in %d ms.", sw.millisSinceStart()));
        } catch (Exception e) {
            //TODO handle this
            throw new RuntimeException(e);
        }

        mIndexingStatus.postValue(new IndexingStatus());
        rebuildAppList();
    }

    @WorkerThread
    private void rebuildAppList() {
        enforceWorkerThread();

        Stopwatch sw = new Stopwatch();

        Map<String, BackupApp> backupApps = new HashMap<>();

        for (PackageMeta packageMeta : mInstalledApps.values()) {

            Backup backup = mIndex.getLatestBackupForPackage(packageMeta.packageName);
            if (backup != null) {
                BackupStatus backupStatus;
                if (backup.versionCode() == packageMeta.versionCode)
                    backupStatus = BackupStatus.SAME_VERSION;
                else if (backup.versionCode() > packageMeta.versionCode)
                    backupStatus = BackupStatus.HIGHER_VERSION;
                else
                    backupStatus = BackupStatus.LOWER_VERSION;

                backupApps.put(packageMeta.packageName, new BackupAppImpl(packageMeta, true, backupStatus));
            } else {
                backupApps.put(packageMeta.packageName, new BackupAppImpl(packageMeta, true, BackupStatus.NO_BACKUP));
            }
        }

        for (String pkg : mIndex.getAllPackages()) {
            if (mInstalledApps.containsKey(pkg))
                continue;

            Backup backup = mIndex.getLatestBackupForPackage(pkg);
            if (backup == null)
                continue;

            backupApps.put(pkg, new BackupAppImpl(backup.toPackageMeta(), false, BackupStatus.APP_NOT_INSTALLED));
        }

        mApps = backupApps;
        mAppsLiveData.postValue(new ArrayList<>(backupApps.values()));

        Log.i(TAG, String.format("Invalidated list in %d ms.", sw.millisSinceStart()));
    }

    @WorkerThread
    private void updateAppInAppList(String pkg) {
        enforceWorkerThread();

        PackageMeta packageMeta = PackageMeta.forPackage(mContext, pkg);
        if (packageMeta != null) {
            mInstalledApps.put(pkg, packageMeta);
        } else {
            mInstalledApps.remove(pkg);
        }
        notifyInstalledAppInvalidated(pkg);

        if (packageMeta != null) {
            Backup backup = mIndex.getLatestBackupForPackage(packageMeta.packageName);
            if (backup != null) {
                BackupStatus backupStatus;
                if (backup.versionCode() == packageMeta.versionCode)
                    backupStatus = BackupStatus.SAME_VERSION;
                else if (backup.versionCode() > packageMeta.versionCode)
                    backupStatus = BackupStatus.HIGHER_VERSION;
                else
                    backupStatus = BackupStatus.LOWER_VERSION;

                mApps.put(packageMeta.packageName, new BackupAppImpl(packageMeta, true, backupStatus));
            } else {
                mApps.put(packageMeta.packageName, new BackupAppImpl(packageMeta, true, BackupStatus.NO_BACKUP));
            }

            mAppsLiveData.postValue(new ArrayList<>(mApps.values()));
            notifyAppInvalidated(pkg);
        } else {
            Backup backup = mIndex.getLatestBackupForPackage(pkg);
            if (backup != null) {
                mApps.put(pkg, new BackupAppImpl(backup.toPackageMeta(), false, BackupStatus.APP_NOT_INSTALLED));
            } else {
                mApps.remove(pkg);
            }

            mAppsLiveData.postValue(new ArrayList<>(mApps.values()));
            notifyAppInvalidated(pkg);
        }
    }

    private void enforceWorkerThread() {
        if (Looper.myLooper() != mWorkerHandler.getLooper())
            throw new RuntimeException("This method must be invoked on mWorkerHandler");
    }

    @Override
    public void onBackupAdded(String storageId, Backup backup) {
        Stopwatch sw = new Stopwatch();

        mIndex.addEntry(backup);
        updateAppInAppList(backup.pkg());

        Log.i(TAG, String.format("onBackupAdded handled in %d ms.", sw.millisSinceStart()));
    }

    @Override
    public void onBackupRemoved(String storageId, Uri backupUri) {
        Stopwatch sw = new Stopwatch();

        Backup backup = mIndex.deleteEntryByUri(mStorage.getStorageId(), backupUri);
        if (backup == null) {
            Log.w(TAG, String.format("Meta from deleteEntryByUri for uri %s in storage %s is null", backupUri.toString(), storageId));
            return;
        }
        updateAppInAppList(backup.pkg());

        Log.i(TAG, String.format("onBackupRemoved handled in %d ms.", sw.millisSinceStart()));
    }

    @Override
    public void onStorageUpdated(String storageId) {
        scanBackups();
    }

    private static class BackupAppImpl implements BackupApp {

        private PackageMeta mPackageMeta;
        private boolean mIsInstalled;
        private BackupStatus mBackupStatus;

        private BackupAppImpl(PackageMeta packageMeta, boolean isInstalled, BackupStatus backupStatus) {
            mPackageMeta = packageMeta;
            mIsInstalled = isInstalled;
            mBackupStatus = backupStatus;
        }

        @Override
        public PackageMeta packageMeta() {
            return mPackageMeta;
        }

        @Override
        public boolean isInstalled() {
            return mIsInstalled;
        }

        @Override
        public BackupStatus backupStatus() {
            return mBackupStatus;
        }
    }

    /**
     * Observers are called on {@link #mWorkerHandler}
     */
    public interface AppsObserver {

        void onInstalledAppInvalidated(String pkg);

        void onAppInvalidated(String pkg);

    }

    private static class BackupAppDetailsImpl implements BackupAppDetails {

        private State mState;
        private BackupApp mApp;
        private List<Backup> mBackups;

        private BackupAppDetailsImpl(State state, BackupApp app, List<Backup> backups) {
            mState = state;
            mApp = app;
            mBackups = backups;
        }

        @Override
        public State state() {
            return mState;
        }

        @Override
        public BackupApp app() {
            return mApp;
        }

        @Override
        public List<Backup> backups() {
            return mBackups;
        }
    }

    private class LiveAppDetails extends LiveData<BackupAppDetails> implements Observer<List<Backup>>, AppsObserver {

        private String mPkg;
        private LiveData<List<Backup>> mMetasLiveData;

        private LiveAppDetails(String pkg) {
            mPkg = pkg;
            setValue(new BackupAppDetailsImpl(BackupAppDetails.State.LOADING, null, null));
        }

        @Override
        protected void onActive() {
            if (mMetasLiveData == null)
                mMetasLiveData = mIndex.getAllBackupsForPackageLiveData(mPkg);

            addAppsObserver(this);
            mMetasLiveData.observeForever(this);
        }

        @Override
        protected void onInactive() {
            mMetasLiveData.removeObserver(this);
            removeAppsObserver(this);

            if (!hasActiveObservers()) {
                mMetasLiveData = null;
            }
        }

        @Override
        public void onChanged(List<Backup> backupFileMetas) {
            invalidate();
        }

        @Override
        public void onInstalledAppInvalidated(String pkg) {

        }

        @Override
        public void onAppInvalidated(String pkg) {
            if (pkg.equals(mPkg))
                invalidate();
        }

        private void invalidate() {
            mWorkerHandler.post(() -> {
                postValue(new BackupAppDetailsImpl(BackupAppDetails.State.READY, getApp(mPkg), mIndex.getAllBackupsForPackage(mPkg)));
            });
        }
    }


}
