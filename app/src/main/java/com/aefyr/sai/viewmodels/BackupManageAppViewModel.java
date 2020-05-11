package com.aefyr.sai.viewmodels;

import android.content.Context;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.aefyr.sai.backup2.BackupAppDetails;
import com.aefyr.sai.backup2.BackupManager;
import com.aefyr.sai.backup2.impl.DefaultBackupManager;

public class BackupManageAppViewModel extends ViewModel {

    private Context mContext;
    private String mPackage;

    private BackupManager mBackupManager;

    private LiveData<BackupAppDetails> mDetailsLiveData;

    public BackupManageAppViewModel(Context appContext, String pkg) {
        mContext = appContext;
        mPackage = pkg;

        mBackupManager = DefaultBackupManager.getInstance(mContext);

        mDetailsLiveData = mBackupManager.getAppDetails(pkg);
    }

    public LiveData<BackupAppDetails> getDetails() {
        return mDetailsLiveData;
    }
}
