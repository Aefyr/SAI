package com.aefyr.sai.viewmodels;

import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.aefyr.sai.backup2.BackupManager;
import com.aefyr.sai.backup2.BackupTaskConfig;
import com.aefyr.sai.backup2.impl.DefaultBackupManager;
import com.aefyr.sai.model.common.PackageMeta;
import com.aefyr.sai.utils.PreferencesHelper;

import java.util.ArrayList;
import java.util.List;

public class BatchBackupDialogViewModel extends ViewModel {
    private static final String TAG = "BatchBackupVM";

    private Context mContext;

    private BackupManager mBackupManager;

    private MutableLiveData<Boolean> mIsPreparing = new MutableLiveData<>();
    private MutableLiveData<Boolean> mIsBackupEnqueued = new MutableLiveData<>();

    private Uri mBackupDirUri;

    private ArrayList<String> mSelectedPackages;

    public BatchBackupDialogViewModel(@NonNull Context applicationContext, @Nullable ArrayList<String> selectedPackages) {
        mContext = applicationContext;
        mBackupManager = DefaultBackupManager.getInstance(mContext);

        mSelectedPackages = selectedPackages;

        mBackupDirUri = PreferencesHelper.getInstance(mContext).getBackupDirUri();

        mIsPreparing.setValue(false);
        mIsBackupEnqueued.setValue(false);
    }

    public LiveData<Boolean> getIsPreparing() {
        return mIsPreparing;
    }

    public LiveData<Boolean> getIsBackupEnqueued() {
        return mIsBackupEnqueued;
    }

    public void enqueueBackup() {
        if (mIsPreparing.getValue())
            return;

        mIsPreparing.setValue(true);
        new Thread(() -> {
            if (mSelectedPackages != null) {
                backupSelectedApps();
            } else {
                backupLiterallyAllSplits();
            }

            mIsBackupEnqueued.postValue(true);
            mIsPreparing.postValue(false);
        }).start();
    }

    private void backupLiterallyAllSplits() {
        List<PackageMeta> packages = DefaultBackupManager.getInstance(mContext).getInstalledPackages().getValue();
        if (packages == null)
            return;

        for (PackageMeta packageMeta : packages) {
            if (!packageMeta.hasSplits)
                continue;

            mBackupManager.enqueueBackup(new BackupTaskConfig.Builder(packageMeta).build());
        }
    }

    private void backupSelectedApps() {
        for (String pkg : mSelectedPackages) {
            PackageMeta packageMeta = PackageMeta.forPackage(mContext, pkg);
            if (packageMeta == null) {
                Log.d(TAG, "PackageMeta is null for " + pkg);
                continue;
            }

            mBackupManager.enqueueBackup(new BackupTaskConfig.Builder(packageMeta).build());
        }
    }

    public boolean doesRequireStoragePermissions() {
        return !ContentResolver.SCHEME_CONTENT.equals(mBackupDirUri.getScheme());
    }

    public int getApkCount() {
        return mSelectedPackages != null ? mSelectedPackages.size() : -1;
    }


}
