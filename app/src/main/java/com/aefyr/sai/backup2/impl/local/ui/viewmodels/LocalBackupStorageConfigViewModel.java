package com.aefyr.sai.backup2.impl.local.ui.viewmodels;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aefyr.sai.backup2.impl.local.LocalBackupStorageProvider;

public class LocalBackupStorageConfigViewModel extends AndroidViewModel implements LocalBackupStorageProvider.OnConfigChangeListener {

    private LocalBackupStorageProvider mProvider;

    private MutableLiveData<Uri> mBackupDirUriLiveData = new MutableLiveData<>();

    public LocalBackupStorageConfigViewModel(@NonNull Application application) {
        super(application);

        mProvider = LocalBackupStorageProvider.getInstance(getApplication());

        mProvider.addOnConfigChangeListener(this, new Handler());
        mBackupDirUriLiveData.setValue(mProvider.getBackupDirUri());
    }

    public LiveData<Uri> getBackupDirUri() {
        return mBackupDirUriLiveData;
    }

    public void setBackupDir(Uri backupDirUri) {
        mProvider.setBackupDirUri(backupDirUri);
    }

    @Override
    protected void onCleared() {
        mProvider.removeOnConfigChangeListener(this);
    }

    @Override
    public void onBackupDirChanged() {
        mBackupDirUriLiveData.setValue(mProvider.getBackupDirUri());
    }
}
