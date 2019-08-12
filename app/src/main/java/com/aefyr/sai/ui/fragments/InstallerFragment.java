package com.aefyr.sai.ui.fragments;

import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProviders;

import com.aefyr.sai.R;
import com.aefyr.sai.ui.activities.MainActivity;
import com.aefyr.sai.ui.dialogs.AppInstalledDialogFragment;
import com.aefyr.sai.ui.dialogs.ErrorLogDialogFragment;
import com.aefyr.sai.ui.dialogs.FilePickerDialogFragment;
import com.aefyr.sai.ui.dialogs.InstallationConfirmationDialogFragment;
import com.aefyr.sai.utils.AlertsUtils;
import com.aefyr.sai.utils.PermissionsUtils;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.viewmodels.InstallerViewModel;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;

import java.io.File;
import java.util.List;

public class InstallerFragment extends SaiBaseFragment implements FilePickerDialogFragment.OnFilesSelectedListener, InstallationConfirmationDialogFragment.ConfirmationListener {

    private InstallerViewModel mViewModel;
    private Button mButton;

    private PreferencesHelper mHelper;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mHelper = PreferencesHelper.getInstance(getContext());

        mButton = findViewById(R.id.button_install);

        mViewModel = ViewModelProviders.of(this).get(InstallerViewModel.class);
        mViewModel.getState().observe(this, (state) -> {
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
        });

        mButton.setOnClickListener((v) -> checkPermissionsAndPickFiles());
        findViewById(R.id.button_help).setOnClickListener((v) -> AlertsUtils.showAlert(this, R.string.help, R.string.installer_help));
    }

    public void onActionView(Uri uri) {
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

    private void showPackageInstalledAlert(String packageName) {
        AppInstalledDialogFragment.newInstance(packageName).show(getChildFragmentManager(), "dialog_app_installed");
    }

    private void setNavigationEnabled(boolean enabled) {
        ((MainActivity) getActivity()).setNavigationEnabled(enabled);
    }

    @Override
    public void onFilesSelected(String tag, List<File> files) {
        if (files.size() == 1 && (files.get(0).getName().endsWith(".zip") || files.get(0).getName().endsWith(".apks"))) {
            mViewModel.installPackagesFromZip(files.get(0));
            return;
        }

        for (File f : files) {
            if (!f.getName().endsWith(".apk")) {
                AlertsUtils.showAlert(this, R.string.error, R.string.installer_error_mixed_extensions);
                return;
            }
        }

        mViewModel.installPackages(files);
    }

    @Override
    public void onConfirmed(Uri apksFileUri) {
        mViewModel.installPackagesFromContentProviderZip(apksFileUri);
    }

    @Override
    protected int layoutId() {
        return R.layout.fragment_installer;
    }
}
