package com.aefyr.sai.ui.dialogs;

import android.content.ContentResolver;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.adapters.BackupSplitPartsAdapter;
import com.aefyr.sai.backup.BackupService;
import com.aefyr.sai.backup.BackupUtils;
import com.aefyr.sai.model.common.PackageMeta;
import com.aefyr.sai.ui.dialogs.base.BaseBottomSheetDialogFragment;
import com.aefyr.sai.utils.PermissionsUtils;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.viewmodels.BackupDialogViewModel;

import java.io.File;
import java.util.List;

public class BackupDialogFragment extends BaseBottomSheetDialogFragment {
    private static final String ARG_PACKAGE = "package";

    private PackageMeta mPackage;
    private BackupDialogViewModel mViewModel;
    private Uri mBackupDirUri;

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

        mBackupDirUri = PreferencesHelper.getInstance(requireContext()).getBackupDirUri();

        mViewModel = ViewModelProviders.of(this).get(BackupDialogViewModel.class);

        Bundle args = getArguments();
        if (args == null)
            return;
        mPackage = args.getParcelable(ARG_PACKAGE);

        if (savedInstanceState == null)
            mViewModel.setPackage(mPackage.packageName);
    }

    @Override
    protected View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        RecyclerView recyclerView = new RecyclerView(requireContext());
        recyclerView.setOverScrollMode(RecyclerView.OVER_SCROLL_NEVER);
        return recyclerView;
    }

    @Override
    protected void onContentViewCreated(View view, @Nullable Bundle savedInstanceState) {
        setTitle(R.string.backup_dialog_title);

        Button enqueueButton = getPositiveButton();
        enqueueButton.setText(R.string.backup_enqueue);
        enqueueButton.setOnClickListener((v) -> enqueueBackup());

        Button cancelButton = getNegativeButton();
        cancelButton.setOnClickListener((v) -> dismiss());

        RecyclerView partsRecycler = (RecyclerView) view;
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
        if (doesRequireStoragePermissions() && !PermissionsUtils.checkAndRequestStoragePermissions(this))
            return;

        List<File> selectedApks = mViewModel.getSelectedSplitParts();

        //TODO probably shouldn't create files on main thread
        Uri backupFileUri = BackupUtils.createBackupFile(requireContext(), mBackupDirUri, mPackage, selectedApks.size() > 1);
        if (backupFileUri == null) {
            showError(R.string.backup_error_cant_mkdir);
            dismiss();
            return;
        }

        BackupService.BackupTaskConfig config = new BackupService.BackupTaskConfig.Builder(mPackage, backupFileUri)
                .addAllApks(selectedApks)
                .setPackApksIntoAnArchive(selectedApks.size() > 1)
                .build();

        BackupService.enqueueBackup(getContext(), config);

        dismiss();
    }

    private boolean doesRequireStoragePermissions() {
        return !ContentResolver.SCHEME_CONTENT.equals(mBackupDirUri.getScheme());
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

    private void showError(@StringRes int message) {
        SimpleAlertDialogFragment.newInstance(getText(R.string.error), getText(message)).show(requireFragmentManager(), null);
    }
}
