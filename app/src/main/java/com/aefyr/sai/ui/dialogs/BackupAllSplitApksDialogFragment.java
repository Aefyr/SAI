package com.aefyr.sai.ui.dialogs;

import android.app.Dialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.aefyr.sai.R;
import com.aefyr.sai.backup.BackupRepository;
import com.aefyr.sai.backup.BackupService;
import com.aefyr.sai.model.backup.PackageMeta;
import com.aefyr.sai.utils.PermissionsUtils;
import com.aefyr.sai.utils.Utils;

import java.io.File;
import java.util.List;

public class BackupAllSplitApksDialogFragment extends DialogFragment {

    private boolean mIsPreparing = false;

    public static BackupAllSplitApksDialogFragment newInstance() {
        BackupAllSplitApksDialogFragment fragment = new BackupAllSplitApksDialogFragment();
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);

        if (savedInstanceState != null) {
            mIsPreparing = savedInstanceState.getBoolean("preparing", false);
        }
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog alertDialog = new AlertDialog.Builder(requireContext())
                .setMessage(R.string.backup_export_all_splits_prompt)
                .setPositiveButton(R.string.yes, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        alertDialog.setOnShowListener(dialog -> bindDialog());

        return alertDialog;
    }

    private void bindDialog() {
        AlertDialog dialog = (AlertDialog) getDialog();

        Button positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE);
        positiveButton.setVisibility(mIsPreparing ? View.GONE : View.VISIBLE);
        positiveButton.setOnClickListener((v) -> {
            if (!PermissionsUtils.checkAndRequestStoragePermissions(this))
                return;

            enqueueBackup();
        });

        Button negativeButton = dialog.getButton(Dialog.BUTTON_NEGATIVE);
        negativeButton.setVisibility(mIsPreparing ? View.GONE : View.VISIBLE);

        dialog.setMessage(getString(mIsPreparing ? R.string.backup_preparing : R.string.backup_export_all_splits_prompt));
    }

    private void enqueueBackup() {
        if (mIsPreparing)
            return;

        mIsPreparing = true;
        bindDialog();

        new Thread(() -> {
            List<PackageMeta> packages = BackupRepository.getInstance(requireContext()).getPackages().getValue();
            if (packages == null)
                return;

            for (PackageMeta packageMeta : packages) {
                if (!packageMeta.hasSplits)
                    continue;

                File backupFile = Utils.createBackupFile(packageMeta);
                if (backupFile == null) {
                    Toast.makeText(requireContext(), getString(R.string.backup_backup_failed, packageMeta.label), Toast.LENGTH_SHORT).show();
                    continue;
                }
                BackupService.enqueueBackup(requireContext(), packageMeta, Uri.fromFile(backupFile));
            }

            new Handler(Looper.getMainLooper()).post(this::dismiss);
        }).start();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionsUtils.REQUEST_CODE_STORAGE_PERMISSIONS) {
            if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED) {
                SimpleAlertDialogFragment.newInstance(getText(R.string.error), getText(R.string.permissions_required_storage)).show(requireFragmentManager(), null);
                dismiss();
            } else
                enqueueBackup();
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("preparing", mIsPreparing);
    }
}
