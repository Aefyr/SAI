package com.aefyr.sai.ui.dialogs;

import android.app.Dialog;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.adapters.BackupSplitPartsAdapter;
import com.aefyr.sai.backup.BackupService;
import com.aefyr.sai.model.backup.PackageMeta;
import com.aefyr.sai.utils.PermissionsUtils;
import com.aefyr.sai.utils.Theme;
import com.aefyr.sai.utils.Utils;
import com.aefyr.sai.viewmodels.BackupDialogViewModel;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

import java.io.File;

public class BackupDialogFragment extends BottomSheetDialogFragment {
    private static final String ARG_PACKAGE = "package";

    private PackageMeta mPackage;
    private BackupDialogViewModel mViewModel;
    private BottomSheetDialog mDialog;

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

        mViewModel = ViewModelProviders.of(this).get(BackupDialogViewModel.class);

        Bundle args = getArguments();
        if (args == null)
            return;
        mPackage = args.getParcelable(ARG_PACKAGE);

        if (savedInstanceState == null)
            mViewModel.setPackage(mPackage.packageName);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        mDialog = new BottomSheetDialog(requireContext(), Theme.getInstance(requireContext()).getCurrentThemeDescriptor().isDark() ? R.style.SAIBottomSheetDialog_Backup : R.style.SAIBottomSheetDialog_Backup_Light);

        View contentView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_backup, null);
        bindDialogContentView(contentView);
        mDialog.setContentView(contentView);

        return mDialog;
    }

    private void bindDialogContentView(View view) {
        Button enqueueButton = view.findViewById(R.id.button_backup);
        enqueueButton.setOnClickListener((v) -> enqueueBackup());

        Button cancelButton = view.findViewById(R.id.button_cancel);
        cancelButton.setOnClickListener((v) -> dismiss());

        RecyclerView partsRecycler = view.findViewById(R.id.rv_split_parts);
        partsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        BackupSplitPartsAdapter adapter = new BackupSplitPartsAdapter(mViewModel.getSelection(), this, requireContext());
        partsRecycler.setAdapter(adapter);


        mViewModel.getLoadingState().observe(this, state -> {
            switch (state) {
                case EMPTY:
                case LOADING:
                    enqueueButton.setEnabled(false);
                    enqueueButton.setText(R.string.backup_splits_loading);
                    break;
                case LOADED:
                    enqueueButton.setEnabled(mViewModel.getSelection().hasSelection());
                    enqueueButton.setText(R.string.backup_enqueue);
                    break;
                case FAILED:
                    showError(R.string.backup_load_splits_error);
                    dismiss();
                    break;
            }
        });
        mViewModel.getSplitParts().observe(this, parts -> {
            adapter.setData(parts);
            revealBottomSheet();
        });
        mViewModel.getSelection().asLiveData().observe(this, selection -> enqueueButton.setEnabled(selection.hasSelection()));
    }

    private void enqueueBackup() {
        if (!PermissionsUtils.checkAndRequestStoragePermissions(this))
            return;

        File backupFile = Utils.createBackupFile(mPackage);
        if (backupFile == null) {
            showError(R.string.backup_error_cant_mkdir);
            dismiss();
            return;
        }

        BackupService.BackupTaskConfig config = new BackupService.BackupTaskConfig.Builder(mPackage, Uri.fromFile(backupFile))
                .addAllApks(mViewModel.getSelectedSplitParts())
                .build();

        BackupService.enqueueBackup(getContext(), config);

        dismiss();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionsUtils.REQUEST_CODE_STORAGE_PERMISSIONS) {
            if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED)
                showError(R.string.permissions_required_storage);
            else
                enqueueBackup();

            dismiss();
        }
    }

    private void revealBottomSheet() {
        FrameLayout bottomSheet = mDialog.findViewById(R.id.design_bottom_sheet);
        BottomSheetBehavior bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet);
        bottomSheetBehavior.setState(BottomSheetBehavior.STATE_EXPANDED);
    }

    private void showError(@StringRes int message) {
        SimpleAlertDialogFragment.newInstance(getText(R.string.error), getText(message)).show(requireFragmentManager(), null);
    }
}
