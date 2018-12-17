package com.aefyr.sai.ui.fragments;

import android.content.Intent;
import android.os.Bundle;

import com.aefyr.sai.R;
import com.aefyr.sai.ui.activities.AboutActivity;
import com.aefyr.sai.ui.activities.PreferencesActivity;
import com.aefyr.sai.ui.dialogs.FilePickerDialogFragment;
import com.aefyr.sai.utils.PermissionsUtils;
import com.aefyr.sai.utils.PreferencesHelper;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;

import java.io.File;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class PreferencesFragment extends PreferenceFragmentCompat {

    private PreferencesHelper mHelper;

    private Preference mHomeDirPref;
    private FilePickerDialogFragment mPendingFilePicker;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_main, rootKey);

        mHelper = PreferencesHelper.getInstance(getContext());

        mHomeDirPref = findPreference("home_directory");
        updateHomeDirPrefSummary();
        mHomeDirPref.setOnPreferenceClickListener((p) -> {
            selectHomeDir();
            return true;
        });

        findPreference("about").setOnPreferenceClickListener((p) -> {
            startActivity(new Intent(getContext(), AboutActivity.class));
            return true;
        });
    }

    private void selectHomeDir() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.DIR_SELECT;
        properties.offset = new File(properties.root, "sdcard");

        openFilePicker(FilePickerDialogFragment.newInstance(PreferencesActivity.FILE_PICKER_TAG_HOME_DIR, getString(R.string.settings_main_pick_dir), properties));
    }

    private void openFilePicker(FilePickerDialogFragment filePicker) {
        if (!PermissionsUtils.checkAndRequestStoragePermissions(this)) {
            mPendingFilePicker = filePicker;
            return;
        }
        filePicker.show(Objects.requireNonNull(getFragmentManager()), null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PermissionsUtils.REQUEST_CODE_STORAGE_PERMISSIONS) {
            if (mPendingFilePicker != null) {
                openFilePicker(mPendingFilePicker);
                mPendingFilePicker = null;
            }
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    public void onHomeDirSelected(File homeDir) {
        mHelper.setHomeDirectory(homeDir.getAbsolutePath());
        updateHomeDirPrefSummary();
    }

    private void updateHomeDirPrefSummary() {
        mHomeDirPref.setSummary(String.format(getString(R.string.settings_main_home_directory_summary), mHelper.getHomeDirectory()));
    }
}
