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
import com.aefyr.sai.utils.Utils;
import com.aefyr.sai.viewmodels.BatchBackupDialogViewModel;
import com.aefyr.sai.viewmodels.factory.BatchBackupDialogViewModelFactory;

import java.util.ArrayList;

public class BatchBackupDialogFragment extends DialogFragment {
    private static final String ARG_PACKAGES = "packages";

    private BatchBackupDialogViewModel mViewModel;

    /**
     * Create a {@link BatchBackupDialogFragment} that will prompt user to backup all split APKs on device
     *
     * @return
     */
    public static BatchBackupDialogFragment newInstance() {
        return new BatchBackupDialogFragment();
    }

    /**
     * Create a {@link BatchBackupDialogFragment} that will prompt user to backup all apps passed in {@code packages}
     *
     * @param packages app packages to backup
     * @return
     */
    public static BatchBackupDialogFragment newInstance(@NonNull ArrayList<String> packages) {
        Bundle args = new Bundle();
        args.putStringArrayList(ARG_PACKAGES, packages);

        BatchBackupDialogFragment dialog = new BatchBackupDialogFragment();
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

        mViewModel = new ViewModelProvider(this, new BatchBackupDialogViewModelFactory(requireContext().getApplicationContext(), packages)).get(BatchBackupDialogViewModel.class);
        mViewModel.getIsBackupEnqueued().observe(this, (isBackupEnqueued) -> {
            if (isBackupEnqueued) {

                OnBatchBackupEnqueuedListener listener = Utils.getParentAs(this, OnBatchBackupEnqueuedListener.class);
                if (listener != null)
                    listener.onBatchBackupEnqueued(getTag());

                dismiss();
            }

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
        mViewModel.enqueueBackup();
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

    public interface OnBatchBackupEnqueuedListener {

        void onBatchBackupEnqueued(@Nullable String dialogTag);

    }
}
