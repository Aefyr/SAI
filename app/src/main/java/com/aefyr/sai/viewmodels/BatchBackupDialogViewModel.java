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

import com.aefyr.sai.backup.BackupRepository;
import com.aefyr.sai.backup.BackupService;
import com.aefyr.sai.backup.BackupUtils;
import com.aefyr.sai.model.common.PackageMeta;
import com.aefyr.sai.utils.PreferencesHelper;

import java.util.ArrayList;
import java.util.List;

public class BatchBackupDialogViewModel extends ViewModel {
    private static final String TAG = "BatchBackupVM";

    private Context mContext;

    private MutableLiveData<Boolean> mIsPreparing = new MutableLiveData<>();
    private MutableLiveData<Boolean> mIsBackupEnqueued = new MutableLiveData<>();

    private Uri mBackupDirUri;

    private ArrayList<String> mSelectedPackages;

    public BatchBackupDialogViewModel(@NonNull Context applicationContext, @Nullable ArrayList<String> selectedPackages) {
        mContext = applicationContext;

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
        List<PackageMeta> packages = BackupRepository.getInstance(mContext).getPackages().getValue();
        if (packages == null)
            return;

        for (PackageMeta packageMeta : packages) {
            if (!packageMeta.hasSplits)
                continue;

            Uri backupFileUri = BackupUtils.createBackupFile(mContext, mBackupDirUri, packageMeta, true);
            if (backupFileUri == null) {
                Log.wtf(TAG, "Unable to create backup file for " + packageMeta.packageName);
                continue;
            }

            BackupService.enqueueBackup(mContext, new BackupService.BackupTaskConfig.Builder(packageMeta, backupFileUri).build());
        }
    }

    private void backupSelectedApps() {
        for (String pkg : mSelectedPackages) {
            PackageMeta packageMeta = PackageMeta.forPackage(mContext, pkg);
            if (packageMeta == null) {
                Log.d(TAG, "PackageMeta is null for " + pkg);
                continue;
            }

            Uri backupFileUri = BackupUtils.createBackupFile(mContext, mBackupDirUri, packageMeta, true);
            if (backupFileUri == null) {
                Log.wtf(TAG, "Unable to create backup file for " + packageMeta.packageName);
                continue;
            }

            BackupService.enqueueBackup(mContext, new BackupService.BackupTaskConfig.Builder(packageMeta, backupFileUri).build());
        }
    }

    public boolean doesRequireStoragePermissions() {
        return !ContentResolver.SCHEME_CONTENT.equals(mBackupDirUri.getScheme());
    }

    public int getApkCount() {
        return mSelectedPackages != null ? mSelectedPackages.size() : -1;
    }


}
