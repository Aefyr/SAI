package com.aefyr.sai.ui.dialogs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aefyr.sai.R;
import com.aefyr.sai.utils.AlertsUtils;
import com.aefyr.sai.utils.PermissionsUtils;
import com.aefyr.sai.utils.Utils;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;

import java.io.File;
import java.util.List;
import java.util.Objects;

public class UriDirectoryPickerDialogFragment extends SingleChoiceListDialogFragment implements FilePickerDialogFragment.OnFilesSelectedListener {
    private static final int REQUEST_CODE_SELECT_BACKUP_DIR = 1334;

    private FilePickerDialogFragment mPendingFilePicker;

    public static UriDirectoryPickerDialogFragment newInstance(Context context) {
        UriDirectoryPickerDialogFragment fragment = new UriDirectoryPickerDialogFragment();

        Bundle args = new Bundle();
        args.putParcelable(ARG_PARAMS, new DialogParams(context.getText(R.string.settings_main_backup_backup_dir_dialog), R.array.backup_dir_selection_methods));
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    protected void deliverSelectionResult(String tag, int selectedItemIndex) {
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
    }

    private void openFilePicker(FilePickerDialogFragment filePicker) {
        if (!PermissionsUtils.checkAndRequestStoragePermissions(this)) {
            mPendingFilePicker = filePicker;
            return;
        }
        filePicker.show(getChildFragmentManager(), null);
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

            onDirectoryPicked(backupDirUri);
        }
    }

    private void onDirectoryPicked(Uri dirUri) {
        OnDirectoryPickedListener listener = Utils.getParentAs(this, OnDirectoryPickedListener.class);
        if (listener != null)
            listener.onDirectoryPicked(getTag(), dirUri);

        dismiss();
    }

    @Override
    public void onFilesSelected(String tag, List<File> files) {
        switch (tag) {
            case "backup_dir":
                onDirectoryPicked(new Uri.Builder()
                        .scheme("file")
                        .path(files.get(0).getAbsolutePath())
                        .build());
                break;
        }
    }

    public interface OnDirectoryPickedListener {

        void onDirectoryPicked(@Nullable String tag, Uri dirUri);

    }
}
