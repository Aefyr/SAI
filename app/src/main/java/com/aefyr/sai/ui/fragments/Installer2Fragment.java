package com.aefyr.sai.ui.fragments;

import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.aefyr.sai.R;
import com.aefyr.sai.adapters.SaiPiSessionsAdapter;
import com.aefyr.sai.ui.activities.MainActivity;
import com.aefyr.sai.ui.activities.PreferencesActivity;
import com.aefyr.sai.ui.dialogs.AppInstalledDialogFragment;
import com.aefyr.sai.ui.dialogs.ErrorLogDialogFragment2;
import com.aefyr.sai.ui.dialogs.FilePickerDialogFragment;
import com.aefyr.sai.ui.dialogs.InstallationConfirmationDialogFragment;
import com.aefyr.sai.ui.dialogs.ThemeSelectionDialogFragment;
import com.aefyr.sai.utils.AlertsUtils;
import com.aefyr.sai.utils.PermissionsUtils;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.utils.Utils;
import com.aefyr.sai.viewmodels.InstallerViewModel;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Installer2Fragment extends SaiBaseFragment implements FilePickerDialogFragment.OnFilesSelectedListener, InstallationConfirmationDialogFragment.ConfirmationListener, SaiPiSessionsAdapter.ActionDelegate {

    private static final int REQUEST_CODE_GET_FILES = 337;

    private InstallerViewModel mViewModel;
    private Button mButton;
    private ImageButton mButtonSettings;

    private PreferencesHelper mHelper;

    private Uri mPendingActionViewUri;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mHelper = PreferencesHelper.getInstance(getContext());

        mButton = findViewById(R.id.button_install);
        mButtonSettings = findViewById(R.id.ib_settings);

        RecyclerView sessionsRecycler = findViewById(R.id.rv_installer_sessions);
        sessionsRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        SaiPiSessionsAdapter sessionsAdapter = new SaiPiSessionsAdapter(requireContext());
        sessionsAdapter.setActionsDelegate(this);
        sessionsRecycler.setAdapter(sessionsAdapter);

        mViewModel = ViewModelProviders.of(this).get(InstallerViewModel.class);
        //TODO do something about this
        /*mViewModel.getState().observe(this, (state) -> {
            switch (state) {
                case IDLE:
                    mButton.setText(R.string.installer_install_apks);
                    mButton.setEnabled(true);
                    setNavigationEnabled(true);
                    break;
                case INSTALLING:
                    mButton.setText(R.string.installer_installation_in_progress);
                    mButton.setEnabled(false);
                    setNavigationEnabled(false);
                    break;
            }
        });
        mViewModel.getEvents().observe(this, (event) -> {
            if (event.isConsumed())
                return;

            String[] eventData = event.consume();
            switch (eventData[0]) {
                case InstallerViewModel.EVENT_PACKAGE_INSTALLED:
                    showPackageInstalledAlert(eventData[1]);
                    break;
                case InstallerViewModel.EVENT_INSTALLATION_FAILED:
                    ErrorLogDialogFragment.newInstance(getString(R.string.installer_installation_failed), eventData[1]).show(getChildFragmentManager(), "installation_error_dialog");
                    break;
            }
        });*/
        mViewModel.getSessions().observe(this, sessionsAdapter::setData);

        findViewById(R.id.ib_toggle_theme).setOnClickListener((v -> new ThemeSelectionDialogFragment().show(getChildFragmentManager(), "theme_selection_dialog")));
        mButtonSettings.setOnClickListener((v) -> PreferencesActivity.open(requireContext(), PreferencesFragment.class, getString(R.string.settings_title)));

        mButton.setOnClickListener((v) -> checkPermissionsAndPickFiles());
        mButton.setOnLongClickListener((v) -> pickFilesWithSaf());
        findViewById(R.id.button_help).setOnClickListener((v) -> AlertsUtils.showAlert(this, R.string.help, R.string.installer_help));

        if (mPendingActionViewUri != null) {
            handleActionView(mPendingActionViewUri);
            mPendingActionViewUri = null;
        }
    }

    public void handleActionView(Uri uri) {
        if (!isAdded()) {
            mPendingActionViewUri = uri;
            return;
        }

        DialogFragment existingDialog = (DialogFragment) getChildFragmentManager().findFragmentByTag("installation_confirmation_dialog");
        if (existingDialog != null)
            existingDialog.dismiss();
        InstallationConfirmationDialogFragment.newInstance(uri).show(getChildFragmentManager(), "installation_confirmation_dialog");
    }

    private void checkPermissionsAndPickFiles() {
        if (!PermissionsUtils.checkAndRequestStoragePermissions(this))
            return;

        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.MULTI_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = Environment.getExternalStorageDirectory();
        properties.offset = new File(mHelper.getHomeDirectory());
        properties.extensions = new String[]{"apk", "zip", "apks"};
        properties.sortBy = mHelper.getFilePickerSortBy();
        properties.sortOrder = mHelper.getFilePickerSortOrder();

        FilePickerDialogFragment.newInstance(null, getString(R.string.installer_pick_apks), properties).show(getChildFragmentManager(), "dialog_files_picker");
    }

    private boolean pickFilesWithSaf() {
        Intent getContentIntent = new Intent(Intent.ACTION_GET_CONTENT);
        getContentIntent.setType("*/*");
        getContentIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(getContentIntent, getString(R.string.installer_pick_apks)), REQUEST_CODE_GET_FILES);

        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PermissionsUtils.REQUEST_CODE_STORAGE_PERMISSIONS) {
            if (grantResults.length == 0 || grantResults[0] == PackageManager.PERMISSION_DENIED)
                AlertsUtils.showAlert(this, R.string.error, R.string.permissions_required_storage);
            else
                checkPermissionsAndPickFiles();
        }

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_GET_FILES) {
            if (resultCode != Activity.RESULT_OK || data == null)
                return;

            //TODO support multiple .apks files here
            if (data.getData() != null) {
                mViewModel.installPackagesFromContentProviderZip(data.getData());
                return;
            }

            if (data.getClipData() != null) {
                ClipData clipData = data.getClipData();
                List<Uri> apkUris = new ArrayList<>(clipData.getItemCount());

                for (int i = 0; i < clipData.getItemCount(); i++)
                    apkUris.add(clipData.getItemAt(i).getUri());

                mViewModel.installPackagesFromContentProviderUris(apkUris);
            }
        }
    }

    private void showPackageInstalledAlert(String packageName) {
        AppInstalledDialogFragment.newInstance(packageName).show(getChildFragmentManager(), "dialog_app_installed");
    }

    private void setNavigationEnabled(boolean enabled) {
        ((MainActivity) requireActivity()).setNavigationEnabled(enabled);

        mButtonSettings.setEnabled(enabled);
        mButtonSettings.animate()
                .alpha(enabled ? 1f : 0.4f)
                .setDuration(300)
                .start();
    }

    @Override
    public void onFilesSelected(String tag, List<File> files) {
        if (files.size() == 0 || !ensureExtensionsConsistency(files)) {
            AlertsUtils.showAlert(this, R.string.error, R.string.installer_error_mixed_extensions);
            return;
        }

        String extension = Utils.getExtension(files.get(0).getName());

        if (".apks".equals(extension)) {
            mViewModel.installPackagesFromZip(files);
        } else if (".apk".equals(extension)) {
            mViewModel.installPackages(files);
        } else {
            AlertsUtils.showAlert(this, R.string.error, R.string.installer_error_mixed_extensions);
        }
    }

    private boolean ensureExtensionsConsistency(List<File> files) {
        String firstFileExtension = Utils.getExtension(files.get(0).getName());
        if (firstFileExtension == null)
            return false;

        for (int i = 1; i < files.size(); i++) {
            if (!files.get(i).getName().endsWith(firstFileExtension))
                return false;
        }

        return true;
    }

    @Override
    public void onConfirmed(Uri apksFileUri) {
        mViewModel.installPackagesFromContentProviderZip(apksFileUri);
    }

    @Override
    protected int layoutId() {
        return R.layout.fragment_installer2;
    }

    @Override
    public void launchApp(String packageName) {
        try {
            PackageManager pm = requireContext().getPackageManager();
            Intent appLaunchIntent = pm.getLaunchIntentForPackage(packageName);
            Objects.requireNonNull(appLaunchIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(appLaunchIntent);
        } catch (Exception e) {
            Log.w("SAI", e);
            Toast.makeText(requireContext(), R.string.installer_unable_to_launch_app, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showError(Exception exception) {
        ErrorLogDialogFragment2.newInstance(getString(R.string.installer_installation_failed), exception, false).show(getChildFragmentManager(), "installation_error_dialog");
    }
}
