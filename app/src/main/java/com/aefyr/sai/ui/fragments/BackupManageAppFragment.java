package com.aefyr.sai.ui.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.PopupMenu;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.adapters.BackupAppDetailsAdapter;
import com.aefyr.sai.adapters.selection.Selection;
import com.aefyr.sai.adapters.selection.SimpleKeyStorage;
import com.aefyr.sai.backup2.Backup;
import com.aefyr.sai.backup2.BackupApp;
import com.aefyr.sai.ui.dialogs.BackupDialogFragment;
import com.aefyr.sai.ui.dialogs.DeleteBackupConfirmationDialog;
import com.aefyr.sai.ui.dialogs.RestoreBackupDialogFragment;
import com.aefyr.sai.utils.Utils;
import com.aefyr.sai.view.coolbar.Coolbar;
import com.aefyr.sai.viewmodels.BackupManageAppViewModel;
import com.aefyr.sai.viewmodels.factory.BackupManageAppViewModelFactory;

public class BackupManageAppFragment extends SaiBaseFragment implements BackupAppDetailsAdapter.ActionDelegate {
    private static final String TAG = "BackupManageAppFragment";

    private static final String EXTRA_PKG = "pkg";

    private BackupManageAppViewModel mViewModel;

    public static BackupManageAppFragment newInstance(String pkg) {
        Bundle args = new Bundle();
        args.putString(EXTRA_PKG, pkg);

        BackupManageAppFragment fragment = new BackupManageAppFragment();
        fragment.setArguments(args);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        String pkg = requireArguments().getString(EXTRA_PKG);
        mViewModel = new ViewModelProvider(this, new BackupManageAppViewModelFactory(requireContext(), pkg)).get(BackupManageAppViewModel.class);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Coolbar coolbar = findViewById(R.id.coolbar_backup_manage_app);
        findViewById(R.id.ib_close).setOnClickListener(v -> dismiss());

        ImageButton moreOptionsButton = findViewById(R.id.ib_app_details_menu);
        moreOptionsButton.setOnClickListener(this::showMenu);

        RecyclerView recycler = findViewById(R.id.rv_backup_app_details);
        recycler.setLayoutManager(new LinearLayoutManager(requireContext()));

        BackupAppDetailsAdapter detailsAdapter = new BackupAppDetailsAdapter(requireContext(), new Selection<>(new SimpleKeyStorage()), this, this);
        recycler.setAdapter(detailsAdapter);

        mViewModel.getDetails().observe(getViewLifecycleOwner(), details -> {
            switch (details.state()) {
                case LOADING:
                    coolbar.setTitle(getString(R.string.backup_app_details_loading));
                    detailsAdapter.setDetails(null);
                    moreOptionsButton.setVisibility(View.GONE);
                    break;
                case READY:
                    coolbar.setTitle(details.app().packageMeta().label);
                    detailsAdapter.setDetails(details);
                    moreOptionsButton.setVisibility(View.VISIBLE);
                    break;
                case ERROR:
                    detailsAdapter.setDetails(null);
                    moreOptionsButton.setVisibility(View.GONE);
                    Toast.makeText(requireContext(), R.string.backup_app_details_error, Toast.LENGTH_LONG).show();
                    dismiss();
                    break;
            }
        });
    }

    private void showMenu(View anchor) {
        PopupMenu popupMenu = new PopupMenu(requireContext(), anchor);
        popupMenu.getMenuInflater().inflate(R.menu.app_details, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener((menuItem) -> {
            switch (menuItem.getItemId()) {
                case R.id.menu_backup_open_app_in_system_settings:
                    openAppInSystemSettings();
                    break;
            }
            return true;
        });

        popupMenu.show();
    }

    private void openAppInSystemSettings() {
        try {
            Intent settingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, new Uri.Builder().scheme("package").opaquePart(mViewModel.getPackage()).build());
            startActivity(settingsIntent);
        } catch (Exception e) {
            Log.w(TAG, "Unable to open app in system settings", e);
            Toast.makeText(requireContext(), R.string.backup_open_app_in_system_settings_error, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected int layoutId() {
        return R.layout.fragment_backup_manage_app;
    }

    @Override
    public void backupApp(BackupApp backupApp) {
        BackupDialogFragment.newInstance(backupApp.packageMeta()).show(getChildFragmentManager(), null);
    }

    @Override
    public void deleteApp(BackupApp backupApp) {
        Intent intent = new Intent(Intent.ACTION_UNINSTALL_PACKAGE);
        intent.setData(new Uri.Builder().scheme("package").opaquePart(backupApp.packageMeta().packageName).build());
        startActivity(intent);
    }

    @Override
    public void installApp(BackupApp backupApp) {
        Backup latestBackup = mViewModel.getLatestBackup();
        if (latestBackup == null)
            return;

        RestoreBackupDialogFragment.newInstance(latestBackup.uri(), latestBackup.creationTime()).show(getChildFragmentManager(), null);
    }

    @Override
    public void restoreBackup(Backup backup) {
        RestoreBackupDialogFragment.newInstance(backup.uri(), backup.creationTime()).show(getChildFragmentManager(), null);
    }

    @Override
    public void deleteBackup(Backup backup) {
        DeleteBackupConfirmationDialog.newInstance(backup.storageId(), backup.uri(), backup.creationTime()).show(getChildFragmentManager(), null);
    }

    private void dismiss() {
        DismissDelegate dismissDelegate = Utils.getParentAs(this, DismissDelegate.class);
        if (dismissDelegate == null)
            throw new RuntimeException("Host of BackupManageAppFragment must implement BackupManageAppFragment.DismissDelegate");

        dismissDelegate.dismiss(this);
    }

    public interface DismissDelegate {

        void dismiss(BackupManageAppFragment fragment);

    }
}
