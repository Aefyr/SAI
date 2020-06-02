package com.aefyr.sai.backup2;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;

public interface BackupStorageProvider {

    String getId();

    String getName();

    boolean isSetup();

    LiveData<Boolean> getIsSetupLiveData();

    Fragment createSetupFragment();

    Fragment createSettingsFragment();

    BackupStorage getStorage();

}
