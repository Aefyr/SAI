package com.aefyr.sai.ui.dialogs;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

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
    private static final String TAG = "BackupDialogFrag";
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

    @SuppressLint("DefaultLocale")
    private File generateBackupFilePath() {
        File backupsDir = new File(Environment.getExternalStorageDirectory(), "SAI");
        if (!backupsDir.exists() && !backupsDir.mkdir()) {
            Log.e(TAG, "Unable to mkdir:" + backupsDir.toString());
            return null;
        }

        String packageInfoPart = String.format("%s-%s", mPackage.packageName, mPackage.versionName).replace('.', ',');
        if (packageInfoPart.length() > 160)
            packageInfoPart = packageInfoPart.substring(0, 160);

        packageInfoPart = Utils.escapeFileName(packageInfoPart);

        return new File(backupsDir, String.format("%s-%d.apks", packageInfoPart, System.currentTimeMillis()));
    }

    private void enqueueBackup() {
        File backupFile = generateBackupFilePath();
        if (backupFile == null) {
            SimpleAlertDialogFragment.newInstance(getText(R.string.error), getText(R.string.backup_error_cant_mkdir)).show(requireFragmentManager(), null);
            return;
        }
        BackupService.enqueueBackup(getContext(), mPackage, Uri.fromFile(generateBackupFilePath()));
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
