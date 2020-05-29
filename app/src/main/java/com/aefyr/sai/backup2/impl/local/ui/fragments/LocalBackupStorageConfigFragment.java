package com.aefyr.sai.backup2.impl.local.ui.fragments;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.aefyr.sai.R;
import com.aefyr.sai.backup2.impl.local.ui.viewmodels.LocalBackupStorageConfigViewModel;
import com.aefyr.sai.ui.dialogs.FilePickerDialogFragment;
import com.aefyr.sai.ui.dialogs.SingleChoiceListDialogFragment;
import com.aefyr.sai.ui.fragments.SaiBaseFragment;
import com.aefyr.sai.utils.AlertsUtils;
import com.aefyr.sai.utils.PermissionsUtils;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class LocalBackupStorageConfigFragment extends SaiBaseFragment implements SingleChoiceListDialogFragment.OnItemSelectedListener, FilePickerDialogFragment.OnFilesSelectedListener {
    private static final int REQUEST_CODE_SELECT_BACKUP_DIR = 1334;

    private LocalBackupStorageConfigViewModel mViewModel;

    private FilePickerDialogFragment mPendingFilePicker;

    @Override
    protected int layoutId() {
        return R.layout.fragment_local_backup_storage_config;
    }

    public static LocalBackupStorageConfigFragment newInstance() {
        return new LocalBackupStorageConfigFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(LocalBackupStorageConfigViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        findViewById(R.id.button_lbs_select_dir).setOnClickListener(v -> selectBackupDir());

        TextView noDirWarningTv = findViewById(R.id.tv_lbs_no_dir_warning);
        TextView dirTv = findViewById(R.id.tv_lbs_dir);

        mViewModel.getBackupDirUri().observe(getViewLifecycleOwner(), backupDirUri -> {
            if (backupDirUri == null) {
                noDirWarningTv.setVisibility(View.VISIBLE);
                dirTv.setVisibility(View.GONE);
            } else {
                noDirWarningTv.setVisibility(View.GONE);
                dirTv.setVisibility(View.VISIBLE);

                dirTv.setText(getString(R.string.backup_lbs_current_dir, backupDirUri.toString()));
            }
        });
    }

    private void selectBackupDir() {
        SingleChoiceListDialogFragment.newInstance(getText(R.string.settings_main_backup_backup_dir_dialog), R.array.backup_dir_selection_methods).show(getChildFragmentManager(), "backup_dir_selection_method");
    }

    private void openFilePicker(FilePickerDialogFragment filePicker) {
        if (!PermissionsUtils.checkAndRequestStoragePermissions(this)) {
            mPendingFilePicker = filePicker;
            return;
        }
        filePicker.show(Objects.requireNonNull(getChildFragmentManager()), null);
    }

    @Override
    public void onItemSelected(String dialogTag, int selectedItemIndex) {
        switch (dialogTag) {
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
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SELECT_BACKUP_DIR) {
            if (resultCode != Activity.RESULT_OK)
                return;

            Objects.requireNonNull(data);
            Uri backupDirUri = Objects.requireNonNull(data.getData());
            requireContext().getContentResolver().takePersistableUriPermission(backupDirUri, Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            mViewModel.setBackupDir(backupDirUri);
        }
    }

    @Override
    public void onFilesSelected(String tag, List<File> files) {
        switch (tag) {
            case "backup_dir":
                mViewModel.setBackupDir(new Uri.Builder()
                        .scheme("file")
                        .path(files.get(0).getAbsolutePath())
                        .build());
                break;
        }
    }
}
