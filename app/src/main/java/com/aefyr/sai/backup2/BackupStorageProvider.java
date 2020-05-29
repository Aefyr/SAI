package com.aefyr.sai.backup2;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;

public interface BackupStorageProvider {

    String getId();

    String getName();

    Fragment createConfigFragment();

    LiveData<Boolean> getIsConfiguredLiveData();

    boolean isConfigured();

    BackupStorage getStorage();

}
