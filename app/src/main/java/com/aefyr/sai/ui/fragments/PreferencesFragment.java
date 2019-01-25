package com.aefyr.sai.ui.fragments;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;

import com.aefyr.sai.R;
import com.aefyr.sai.ui.activities.AboutActivity;
import com.aefyr.sai.ui.dialogs.FilePickerDialogFragment;
import com.aefyr.sai.ui.dialogs.SingleChoiceListDialogFragment;
import com.aefyr.sai.utils.AlertsUtils;
import com.aefyr.sai.utils.PermissionsUtils;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.utils.Root;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;

import java.io.File;
import java.util.List;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public class PreferencesFragment extends PreferenceFragmentCompat implements FilePickerDialogFragment.OnFilesSelectedListener, SingleChoiceListDialogFragment.OnItemSelectedListener {

    private PreferencesHelper mHelper;

    private Preference mHomeDirPref;
    private Preference mFilePickerSortPref;

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

        mFilePickerSortPref = findPreference("file_picker_sort");
        updateFilePickerSortSummary();
        mFilePickerSortPref.setOnPreferenceClickListener((p) -> {
            SingleChoiceListDialogFragment.newInstance("sort", R.array.file_picker_sort_variants, mHelper.getFilePickerRawSort()).show(getChildFragmentManager(), null);
            return true;
        });

        findPreference("about").setOnPreferenceClickListener((p) -> {
            startActivity(new Intent(getContext(), AboutActivity.class));
            return true;
        });

        findPreference("use_root").setOnPreferenceChangeListener((preference, newValue) -> {
            if ((boolean) newValue) {
                if (!Root.requestRoot()) {
                    AlertsUtils.showAlert(this, R.string.error, R.string.settings_main_use_root_error);
                    return false;
                }
            }
            return true;
        });
    }

    private void openFilePicker(FilePickerDialogFragment filePicker) {
        if (!PermissionsUtils.checkAndRequestStoragePermissions(this)) {
            mPendingFilePicker = filePicker;
            return;
        }
        filePicker.show(Objects.requireNonNull(getChildFragmentManager()), null);
    }

    private void selectHomeDir() {
        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.SINGLE_MODE;
        properties.selection_type = DialogConfigs.DIR_SELECT;
        properties.root = Environment.getExternalStorageDirectory();

        openFilePicker(FilePickerDialogFragment.newInstance("home", getString(R.string.settings_main_pick_dir), properties));
    }

    private void updateHomeDirPrefSummary() {
        mHomeDirPref.setSummary(getString(R.string.settings_main_home_directory_summary, mHelper.getHomeDirectory()));
    }

    private void updateFilePickerSortSummary() {
        mFilePickerSortPref.setSummary(getString(R.string.settings_main_file_picker_sort_summary, getResources().getStringArray(R.array.file_picker_sort_variants)[mHelper.getFilePickerRawSort()]));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionsUtils.REQUEST_CODE_STORAGE_PERMISSIONS) {
            if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED)
                AlertsUtils.showAlert(this, R.string.error, R.string.permissions_required_storage);
            else {
                if (mPendingFilePicker != null) {
                    openFilePicker(mPendingFilePicker);
                    mPendingFilePicker = null;
                }
            }
        }
    }

    @Override
    public void onFilesSelected(String tag, List<File> files) {
        switch (tag) {
            case "home":
                mHelper.setHomeDirectory(files.get(0).getAbsolutePath());
                updateHomeDirPrefSummary();
                break;
        }
    }

    @Override
    public void onItemSelected(String dialogTag, int selectedItemIndex) {
        switch (dialogTag) {
            case "sort":
                mHelper.setFilePickerRawSort(selectedItemIndex);
                switch (selectedItemIndex) {
                    case 0:
                        mHelper.setFilePickerSortBy(DialogConfigs.SORT_BY_NAME);
                        mHelper.setFilePickerSortOrder(DialogConfigs.SORT_ORDER_NORMAL);
                        break;
                    case 1:
                        mHelper.setFilePickerSortBy(DialogConfigs.SORT_BY_NAME);
                        mHelper.setFilePickerSortOrder(DialogConfigs.SORT_ORDER_REVERSE);
                        break;
                    case 2:
                        mHelper.setFilePickerSortBy(DialogConfigs.SORT_BY_LAST_MODIFIED);
                        mHelper.setFilePickerSortOrder(DialogConfigs.SORT_ORDER_NORMAL);
                        break;
                    case 3:
                        mHelper.setFilePickerSortBy(DialogConfigs.SORT_BY_LAST_MODIFIED);
                        mHelper.setFilePickerSortOrder(DialogConfigs.SORT_ORDER_REVERSE);
                        break;
                }
                updateFilePickerSortSummary();
                break;
        }
    }
}
