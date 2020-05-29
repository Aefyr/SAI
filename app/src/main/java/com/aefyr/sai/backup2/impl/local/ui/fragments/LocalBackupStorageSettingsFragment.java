package com.aefyr.sai.backup2.impl.local.ui.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import com.aefyr.sai.R;
import com.aefyr.sai.backup2.impl.local.LocalBackupStorageProvider;
import com.aefyr.sai.backup2.impl.local.prefs.LocalBackupStoragePrefConstants;
import com.aefyr.sai.model.common.PackageMeta;
import com.aefyr.sai.ui.dialogs.NameFormatBuilderDialogFragment;
import com.aefyr.sai.ui.dialogs.UriDirectoryPickerDialogFragment;
import com.aefyr.sai.utils.BackupNameFormat;

import java.util.Objects;

public class LocalBackupStorageSettingsFragment extends PreferenceFragmentCompat implements UriDirectoryPickerDialogFragment.OnDirectoryPickedListener, NameFormatBuilderDialogFragment.OnFormatBuiltListener {

    private Preference mBackupNameFormatPref;
    private Preference mBackupDirPref;

    private LocalBackupStorageProvider mProvider;

    private PackageMeta mDemoMeta;

    public static LocalBackupStorageSettingsFragment newInstance() {
        return new LocalBackupStorageSettingsFragment();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setDividerHeight(0);
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        mProvider = LocalBackupStorageProvider.getInstance(requireContext());
        mDemoMeta = Objects.requireNonNull(PackageMeta.forPackage(requireContext(), requireContext().getPackageName()));

        PreferenceManager prefManager = getPreferenceManager();
        prefManager.setSharedPreferencesName(LocalBackupStoragePrefConstants.PREFS_NAME);

        addPreferencesFromResource(R.xml.preferences_lbs);

        mBackupNameFormatPref = findPreference("backup_file_name_format");
        updateBackupNameFormatSummary();
        mBackupNameFormatPref.setOnPreferenceClickListener((p) -> {
            NameFormatBuilderDialogFragment.newInstance(mProvider.getBackupNameFormat()).show(getChildFragmentManager(), "backup_name_format_builder");
            return true;
        });

        mBackupDirPref = findPreference(LocalBackupStoragePrefConstants.KEY_BACKUP_DIR_URI);
        updateBackupDirSummary();
        mBackupDirPref.setOnPreferenceClickListener(p -> {
            UriDirectoryPickerDialogFragment.newInstance(requireContext()).show(getChildFragmentManager(), "backup_dir");
            return true;
        });
    }

    private void updateBackupNameFormatSummary() {
        mBackupNameFormatPref.setSummary(getString(R.string.settings_main_backup_file_name_format_summary, BackupNameFormat.format(mProvider.getBackupNameFormat(), mDemoMeta)));
    }

    private void updateBackupDirSummary() {
        mBackupDirPref.setSummary(getString(R.string.settings_main_backup_backup_dir_summary, mProvider.getBackupDirUri()));
    }

    @Override
    public void onDirectoryPicked(@Nullable String tag, Uri dirUri) {
        if (tag == null)
            return;

        switch (tag) {
            case "backup_dir":
                mProvider.setBackupDirUri(dirUri);
                updateBackupDirSummary();
                break;
        }
    }

    @Override
    public void onFormatBuilt(@Nullable String tag, @NonNull String format) {
        if (tag == null)
            return;

        switch (tag) {
            case "backup_name_format_builder":
                mProvider.setBackupNameFormat(format);
                updateBackupNameFormatSummary();
                break;
        }
    }
}
