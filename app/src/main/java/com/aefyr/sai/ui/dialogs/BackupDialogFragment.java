package com.aefyr.sai.ui.dialogs;

import android.app.Dialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.aefyr.sai.R;
import com.aefyr.sai.backup.BackupService;
import com.aefyr.sai.model.backup.PackageMeta;
import com.aefyr.sai.utils.PermissionsUtils;
import com.aefyr.sai.utils.Utils;

import java.io.File;

public class BackupDialogFragment extends DialogFragment {
    private static final String ARG_PACKAGE = "package";

    private PackageMeta mPackage;

    public static BackupDialogFragment newInstance(PackageMeta packageMeta) {
        Bundle args = new Bundle();
        args.putParcelable(ARG_PACKAGE, packageMeta);

        BackupDialogFragment fragment = new BackupDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        if (args == null)
            return;
        mPackage = args.getParcelable(ARG_PACKAGE);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog alertDialog = new AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.backup_backup_prompt, mPackage.label))
                .setPositiveButton(R.string.yes, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        alertDialog.setOnShowListener(dialog -> alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener((v) -> {
            if (!PermissionsUtils.checkAndRequestStoragePermissions(this))
                return;

            enqueueBackup();
            dismiss();
        }));

        return alertDialog;
    }

    private void enqueueBackup() {
        File backupFile = Utils.createBackupFile(mPackage);
        if (backupFile == null) {
            SimpleAlertDialogFragment.newInstance(getText(R.string.error), getText(R.string.backup_error_cant_mkdir)).show(requireFragmentManager(), null);
            return;
        }
        BackupService.enqueueBackup(getContext(), mPackage, Uri.fromFile(backupFile));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionsUtils.REQUEST_CODE_STORAGE_PERMISSIONS) {
            if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED)
                SimpleAlertDialogFragment.newInstance(getText(R.string.error), getText(R.string.permissions_required_storage)).show(requireFragmentManager(), null);
            else
                enqueueBackup();

            dismiss();
        }
    }
}
