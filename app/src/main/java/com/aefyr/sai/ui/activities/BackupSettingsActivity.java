package com.aefyr.sai.ui.activities;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import com.aefyr.sai.R;
import com.aefyr.sai.backup2.BackupStorageProvider;
import com.aefyr.sai.backup2.impl.DefaultBackupManager;

public class BackupSettingsActivity extends ThemedActivity {

    private static final String FRAGMENT_TAG = "whatever";

    Fragment mCurrentFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_settings);

        mCurrentFragment = getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG);

        BackupStorageProvider storageProvider = DefaultBackupManager.getInstance(this).getDefaultBackupStorageProvider();

        //TODO probably cache current fragment type to avoid creating a new fragment on activity recreation
        storageProvider.getIsSetupLiveData().observe(this, isSetup -> {
            if (mCurrentFragment != null) {
                getSupportFragmentManager().beginTransaction()
                        .remove(mCurrentFragment)
                        .commitNow();

                mCurrentFragment = null;
            }

            mCurrentFragment = isSetup ? storageProvider.createSettingsFragment() : storageProvider.createSetupFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container_backup_settings, mCurrentFragment, FRAGMENT_TAG)
                    .commitNow();
        });
    }
}