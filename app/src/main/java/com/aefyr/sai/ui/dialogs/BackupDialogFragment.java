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
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.adapters.BackupSplitPartsAdapter;
import com.aefyr.sai.backup2.BackupTaskConfig;
import com.aefyr.sai.backup2.impl.DefaultBackupManager;
import com.aefyr.sai.model.common.PackageMeta;
import com.aefyr.sai.ui.dialogs.base.BaseBottomSheetDialogFragment;
import com.aefyr.sai.utils.PermissionsUtils;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.view.ViewSwitcherLayout;
import com.aefyr.sai.viewmodels.BackupDialogViewModel;

import java.io.File;
import java.util.List;
import java.util.Objects;

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

        mViewModel = new ViewModelProvider(this).get(BackupDialogViewModel.class);

        Bundle args = getArguments();
        if (args == null)
            return;
        mPackage = Objects.requireNonNull(args.getParcelable(ARG_PACKAGE));

        if (savedInstanceState == null)
            mViewModel.setPackage(mPackage.packageName);
    }

    @Override
    protected View onCreateContentView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_backup, container, false);
    }

    @Override
    protected void onContentViewCreated(View view, @Nullable Bundle savedInstanceState) {
        setTitle(R.string.backup_dialog_title);

        Button enqueueButton = getPositiveButton();
        enqueueButton.setText(R.string.backup_enqueue);
        enqueueButton.setOnClickListener((v) -> enqueueBackup());

        Button cancelButton = getNegativeButton();
        cancelButton.setOnClickListener((v) -> dismiss());

        RecyclerView partsRecycler = view.findViewById(R.id.rv_backup_dialog);
        partsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        BackupSplitPartsAdapter adapter = new BackupSplitPartsAdapter(mViewModel.getSelection(), this, requireContext());
        partsRecycler.setAdapter(adapter);

        ViewSwitcherLayout viewSwitcher = view.findViewById(R.id.container_backup_dialog);

        mViewModel.getLoadingState().observe(this, state -> {
            switch (state) {
                case EMPTY:
                case LOADING:
                    viewSwitcher.setShownView(R.id.container_backup_dialog_loading);
                    enqueueButton.setVisibility(View.GONE);
                    break;
                case LOADED:
                    viewSwitcher.setShownView(R.id.rv_backup_dialog);
                    enqueueButton.setVisibility(View.VISIBLE);
                    break;
                case FAILED:
                    viewSwitcher.setShownView(R.id.container_backup_dialog_error);
                    enqueueButton.setVisibility(View.GONE);
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

        BackupTaskConfig config = new BackupTaskConfig.Builder(mPackage)
                .addAllApks(selectedApks)
                .setPackApksIntoAnArchive(selectedApks.size() > 1)
                .build();

        DefaultBackupManager.getInstance(requireContext()).enqueueBackup(config);

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
        SimpleAlertDialogFragment.newInstance(getText(R.string.error), getText(message)).show(getParentFragmentManager(), null);
    }
}
