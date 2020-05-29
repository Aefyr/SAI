package com.aefyr.sai.backup2.impl.local.ui.fragments;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;

import com.aefyr.sai.R;
import com.aefyr.sai.backup2.impl.local.ui.viewmodels.LocalBackupStorageSetupViewModel;
import com.aefyr.sai.ui.dialogs.UriDirectoryPickerDialogFragment;
import com.aefyr.sai.ui.fragments.SaiBaseFragment;

public class LocalBackupStorageSetupFragment extends SaiBaseFragment implements UriDirectoryPickerDialogFragment.OnDirectoryPickedListener {
    private LocalBackupStorageSetupViewModel mViewModel;

    @Override
    protected int layoutId() {
        return R.layout.fragment_local_backup_storage_setup;
    }

    public static LocalBackupStorageSetupFragment newInstance() {
        return new LocalBackupStorageSetupFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewModel = new ViewModelProvider(this).get(LocalBackupStorageSetupViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        findViewById(R.id.button_lbs_select_dir).setOnClickListener(v -> selectBackupDir());
    }

    private void selectBackupDir() {
        UriDirectoryPickerDialogFragment.newInstance(requireContext()).show(getChildFragmentManager(), "backup_dir");
    }

    @Override
    public void onDirectoryPicked(@Nullable String tag, Uri dirUri) {
        if (tag == null)
            return;

        switch (tag) {
            case "backup_dir":
                mViewModel.setBackupDir(dirUri);
                break;
        }
    }
}
