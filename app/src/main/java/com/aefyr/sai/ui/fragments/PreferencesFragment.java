package com.aefyr.sai.ui.fragments;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.aefyr.sai.R;
import com.aefyr.sai.model.common.PackageMeta;
import com.aefyr.sai.shell.SuShell;
import com.aefyr.sai.ui.activities.AboutActivity;
import com.aefyr.sai.ui.dialogs.FilePickerDialogFragment;
import com.aefyr.sai.ui.dialogs.NameFormatBuilderDialogFragment;
import com.aefyr.sai.ui.dialogs.SingleChoiceListDialogFragment;
import com.aefyr.sai.ui.dialogs.ThemeSelectionDialogFragment;
import com.aefyr.sai.ui.dialogs.base.BaseBottomSheetDialogFragment;
import com.aefyr.sai.utils.AlertsUtils;
import com.aefyr.sai.utils.BackupNameFormat;
import com.aefyr.sai.utils.PermissionsUtils;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.utils.PreferencesKeys;
import com.aefyr.sai.utils.PreferencesValues;
import com.aefyr.sai.utils.Theme;
import com.aefyr.sai.utils.Utils;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;

import java.io.File;
import java.util.List;
import java.util.Objects;

import moe.shizuku.api.ShizukuClientHelper;

public class PreferencesFragment extends PreferenceFragmentCompat implements FilePickerDialogFragment.OnFilesSelectedListener, SingleChoiceListDialogFragment.OnItemSelectedListener, BaseBottomSheetDialogFragment.OnDismissListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final int REQUEST_CODE_SELECT_BACKUP_DIR = 1334;

    private PreferencesHelper mHelper;

    private Preference mHomeDirPref;
    private Preference mFilePickerSortPref;
    private Preference mInstallerPref;
    private Preference mBackupNameFormatPref;
    private Preference mBackupDirPref;
    private Preference mThemePref;

    private PackageMeta mDemoMeta;

    private FilePickerDialogFragment mPendingFilePicker;

    @SuppressLint("ApplySharedPref")
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.preferences_main, rootKey);

        mHelper = PreferencesHelper.getInstance(requireContext());
        mDemoMeta = Objects.requireNonNull(PackageMeta.forPackage(requireContext(), requireContext().getPackageName()));

        mHomeDirPref = findPreference("home_directory");
        updateHomeDirPrefSummary();
        mHomeDirPref.setOnPreferenceClickListener((p) -> {
            selectHomeDir();
            return true;
        });

        mFilePickerSortPref = findPreference("file_picker_sort");
        updateFilePickerSortSummary();
        mFilePickerSortPref.setOnPreferenceClickListener((p) -> {
            SingleChoiceListDialogFragment.newInstance(getText(R.string.settings_main_file_picker_sort), R.array.file_picker_sort_variants, mHelper.getFilePickerRawSort()).show(getChildFragmentManager(), "sort");
            return true;
        });

        findPreference("about").setOnPreferenceClickListener((p) -> {
            startActivity(new Intent(getContext(), AboutActivity.class));
            return true;
        });

        mInstallerPref = findPreference("installer");
        updateInstallerSummary();
        mInstallerPref.setOnPreferenceClickListener((p -> {
            SingleChoiceListDialogFragment.newInstance(getText(R.string.settings_main_installer), R.array.installers, mHelper.getInstaller()).show(getChildFragmentManager(), "installer");
            return true;
        }));

        mBackupNameFormatPref = findPreference("backup_file_name_format");
        updateBackupNameFormatSummary();
        mBackupNameFormatPref.setOnPreferenceClickListener((p) -> {
            NameFormatBuilderDialogFragment.newInstance().show(getChildFragmentManager(), "backup_name_format_builder");
            return true;
        });

        mBackupDirPref = findPreference(PreferencesKeys.BACKUP_DIR);
        updateBackupDirSummary();
        mBackupDirPref.setOnPreferenceClickListener(p -> {
            selectBackupDir();
            return true;
        });

        mThemePref = findPreference(PreferencesKeys.THEME);
        updateThemeSummary();
        mThemePref.setOnPreferenceClickListener(p -> {
            ThemeSelectionDialogFragment.newInstance(requireContext()).show(getChildFragmentManager(), "theme");
            return true;
        });

        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setDividerHeight(0);
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

    private void selectBackupDir() {
        SingleChoiceListDialogFragment.newInstance(getText(R.string.settings_main_backup_backup_dir_dialog), R.array.backup_dir_selection_methods).show(getChildFragmentManager(), "backup_dir_selection_method");
    }

    private void updateHomeDirPrefSummary() {
        mHomeDirPref.setSummary(getString(R.string.settings_main_home_directory_summary, mHelper.getHomeDirectory()));
    }

    private void updateFilePickerSortSummary() {
        mFilePickerSortPref.setSummary(getString(R.string.settings_main_file_picker_sort_summary, getResources().getStringArray(R.array.file_picker_sort_variants)[mHelper.getFilePickerRawSort()]));
    }

    private void updateInstallerSummary() {
        mInstallerPref.setSummary(getString(R.string.settings_main_installer_summary, getResources().getStringArray(R.array.installers)[mHelper.getInstaller()]));
    }

    private void updateBackupNameFormatSummary() {
        mBackupNameFormatPref.setSummary(getString(R.string.settings_main_backup_file_name_format_summary, BackupNameFormat.format(mHelper.getBackupFileNameFormat(), mDemoMeta)));
    }

    private void updateBackupDirSummary() {
        mBackupDirPref.setSummary(getString(R.string.settings_main_backup_backup_dir_summary, mHelper.getBackupDirUri()));
    }

    private void updateThemeSummary() {
        mThemePref.setSummary(getResources().getStringArray(R.array.themes)[Theme.getInstance(requireContext()).getCurrentThemeId()]);
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

        if (requestCode == PermissionsUtils.REQUEST_CODE_SHIZUKU) {
            if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED)
                AlertsUtils.showAlert(this, R.string.error, R.string.permissions_required_shizuku);
            else {
                mHelper.setInstaller(PreferencesValues.INSTALLER_SHIZUKU);
                updateInstallerSummary();
            }
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_BACKUP_DIR) {
            if (resultCode != Activity.RESULT_OK)
                return;

            data = Objects.requireNonNull(data);
            Uri backupDirUri = Objects.requireNonNull(data.getData());
            requireContext().getContentResolver().takePersistableUriPermission(backupDirUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            mHelper.setBackupDirUri(backupDirUri.toString());
            updateBackupDirSummary();
        }
    }

    @Override
    public void onFilesSelected(String tag, List<File> files) {
        switch (tag) {
            case "home":
                mHelper.setHomeDirectory(files.get(0).getAbsolutePath());
                updateHomeDirPrefSummary();
                break;
            case "backup_dir":
                mHelper.setBackupDirUri(new Uri.Builder()
                        .scheme("file")
                        .path(files.get(0).getAbsolutePath())
                        .build()
                        .toString());
                updateBackupDirSummary();
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
                    case 4:
                        mHelper.setFilePickerSortBy(DialogConfigs.SORT_BY_SIZE);
                        mHelper.setFilePickerSortOrder(DialogConfigs.SORT_ORDER_REVERSE);
                        break;
                    case 5:
                        mHelper.setFilePickerSortBy(DialogConfigs.SORT_BY_SIZE);
                        mHelper.setFilePickerSortOrder(DialogConfigs.SORT_ORDER_NORMAL);
                        break;
                }
                updateFilePickerSortSummary();
                break;
            case "installer":
                boolean installerSet = false;
                switch (selectedItemIndex) {
                    case PreferencesValues.INSTALLER_ROOTLESS:
                        installerSet = true;
                        break;
                    case PreferencesValues.INSTALLER_ROOTED:
                        if (!SuShell.getInstance().requestRoot()) {
                            AlertsUtils.showAlert(this, R.string.error, R.string.settings_main_use_root_error);
                            return;
                        }
                        installerSet = true;
                        break;
                    case PreferencesValues.INSTALLER_SHIZUKU:
                        if (!Utils.apiIsAtLeast(Build.VERSION_CODES.M)) {
                            AlertsUtils.showAlert(this, R.string.error, R.string.settings_main_installer_error_shizuku_pre_m);
                            return;
                        }
                        if (!ShizukuClientHelper.isManagerV3Installed(requireContext())) {
                            AlertsUtils.showAlert(this, R.string.error, R.string.settings_main_installer_error_no_shizuku);
                            return;
                        }

                        installerSet = PermissionsUtils.checkAndRequestShizukuPermissions(this);
                        break;
                }
                if (installerSet) {
                    mHelper.setInstaller(selectedItemIndex);
                    updateInstallerSummary();
                }
                break;
            case "backup_dir_selection_method":
                switch (selectedItemIndex) {
                    case 0:
                        DialogProperties properties = new DialogProperties();
                        properties.selection_mode = DialogConfigs.SINGLE_MODE;
                        properties.selection_type = DialogConfigs.DIR_SELECT;
                        properties.root = Environment.getExternalStorageDirectory();

                        openFilePicker(FilePickerDialogFragment.newInstance("backup_dir", getString(R.string.settings_main_pick_dir), properties));
                        break;
                    case 1:
                        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                        startActivityForResult(Intent.createChooser(intent, getString(R.string.installer_pick_apks)), REQUEST_CODE_SELECT_BACKUP_DIR);
                        break;
                }
                break;
        }
    }

    @Override
    public void onDialogDismissed(@NonNull String dialogTag) {
        switch (dialogTag) {
            case "backup_name_format_builder":
                updateBackupNameFormatSummary();
                break;
            case "theme":
                updateThemeSummary();
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }

    @SuppressLint("ApplySharedPref")
    @Override
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
        if (key.equals(PreferencesKeys.USE_OLD_INSTALLER)) {
            prefs.edit().putBoolean(PreferencesKeys.USE_OLD_INSTALLER, prefs.getBoolean(PreferencesKeys.USE_OLD_INSTALLER, false)).commit();
            Utils.hardRestartApp(requireContext());
        }
    }
}
