package com.aefyr.sai.viewmodels;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.aefyr.sai.backup2.Backup;
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

    public String getPackage() {
        return mPackage;
    }

    @Nullable
    public Backup getLatestBackup() {
        BackupAppDetails details = mDetailsLiveData.getValue();
        if (details == null)
            return null;

        if (details.backups().size() > 0)
            return details.backups().get(0);

        return null;
    }
}
