package com.aefyr.sai.viewmodels;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aefyr.sai.backup.BackupRepository;
import com.aefyr.sai.backup.BackupService;
import com.aefyr.sai.model.common.PackageMeta;
import com.aefyr.sai.utils.Utils;

import java.io.File;
import java.util.List;

public class BackupAllSplitApksDialogViewModel extends AndroidViewModel {

    private MutableLiveData<Boolean> mIsPreparing = new MutableLiveData<>();
    private MutableLiveData<Boolean> mIsBackupEnqueued = new MutableLiveData<>();

    public BackupAllSplitApksDialogViewModel(@NonNull Application application) {
        super(application);
        mIsPreparing.setValue(false);
        mIsBackupEnqueued.setValue(false);
    }

    public LiveData<Boolean> getIsPreparing() {
        return mIsPreparing;
    }

    public LiveData<Boolean> getIsBackupEnqueued() {
        return mIsBackupEnqueued;
    }

    public void backupAllSplits() {
        if (mIsPreparing.getValue())
            return;

        mIsPreparing.setValue(true);
        new Thread(() -> {
            List<PackageMeta> packages = BackupRepository.getInstance(getApplication()).getPackages().getValue();
            if (packages == null)
                return;

            for (PackageMeta packageMeta : packages) {
                if (!packageMeta.hasSplits)
                    continue;

                File backupFile = Utils.createBackupFile(getApplication(), packageMeta);
                if (backupFile == null) {
                    Log.wtf("BackupAllSplits", "Unable to create backup file for " + packageMeta.packageName);
                    continue;
                }

                BackupService.enqueueBackup(getApplication(), new BackupService.BackupTaskConfig.Builder(packageMeta, Uri.fromFile(backupFile)).build());
            }

            mIsBackupEnqueued.postValue(true);
            mIsPreparing.postValue(false);
        }).start();
    }


}
