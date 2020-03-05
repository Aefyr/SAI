package com.aefyr.sai.ui.dialogs;

import android.app.Dialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;

import com.aefyr.sai.R;
import com.aefyr.sai.utils.PermissionsUtils;
import com.aefyr.sai.viewmodels.BackupAllSplitApksDialogViewModel;
import com.aefyr.sai.viewmodels.factory.BackupAllSplitApksDialogViewModelFactory;

import java.util.ArrayList;

public class BackupAllSplitApksDialogFragment extends DialogFragment {
    private static final String ARG_PACKAGES = "packages";

    private BackupAllSplitApksDialogViewModel mViewModel;

    public static BackupAllSplitApksDialogFragment newInstance() {
        return new BackupAllSplitApksDialogFragment();
    }

    public static BackupAllSplitApksDialogFragment newInstance(ArrayList<String> packages) {
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_PACKAGES, packages);

        BackupAllSplitApksDialogFragment dialog = new BackupAllSplitApksDialogFragment();
        dialog.setArguments(args);

        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setCancelable(false);

        Bundle args = getArguments();
        ArrayList<String> packages = null;
        if (args != null) {
            packages = args.getStringArrayList(ARG_PACKAGES);
        }

        mViewModel = new ViewModelProvider(this, new BackupAllSplitApksDialogViewModelFactory(requireContext().getApplicationContext(), packages)).get(BackupAllSplitApksDialogViewModel.class);
        mViewModel.getIsBackupEnqueued().observe(this, (isBackupEnqueued) -> {
            if (isBackupEnqueued)
                dismiss();
        });
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        AlertDialog alertDialog = new AlertDialog.Builder(requireContext())
                .setMessage(getExportPromptText())
                .setPositiveButton(R.string.yes, null)
                .setNegativeButton(R.string.cancel, null)
                .create();

        alertDialog.setOnShowListener(dialog -> bindDialog(alertDialog));

        return alertDialog;
    }

    private void bindDialog(AlertDialog dialog) {
        Button positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(Dialog.BUTTON_NEGATIVE);

        positiveButton.setOnClickListener((v) -> {
            if (mViewModel.doesRequireStoragePermissions() && !PermissionsUtils.checkAndRequestStoragePermissions(this))
                return;

            enqueueBackup();
        });

        mViewModel.getIsPreparing().observe(this, (isPreparing) -> {
            positiveButton.setVisibility(isPreparing ? View.GONE : View.VISIBLE);
            negativeButton.setVisibility(isPreparing ? View.GONE : View.VISIBLE);

            dialog.setMessage(isPreparing ? getString(R.string.backup_preparing) : getExportPromptText());
        });
    }

    private void enqueueBackup() {
        mViewModel.backupAllSplits();
    }

    private String getExportPromptText() {
        return mViewModel.getApkCount() <= 0 ? getString(R.string.backup_export_all_splits_prompt) : getString(R.string.backup_export_selected_apps_prompt, mViewModel.getApkCount());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionsUtils.REQUEST_CODE_STORAGE_PERMISSIONS) {
            if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED) {
                SimpleAlertDialogFragment.newInstance(getText(R.string.error), getText(R.string.permissions_required_storage)).show(getParentFragmentManager(), null);
                dismiss();
            } else
                enqueueBackup();
        }
    }
}
