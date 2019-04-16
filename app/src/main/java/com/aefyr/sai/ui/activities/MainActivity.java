package com.aefyr.sai.ui.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ImageButton;

import com.aefyr.sai.R;
import com.aefyr.sai.ui.dialogs.AppInstalledDialogFragment;
import com.aefyr.sai.ui.dialogs.FilePickerDialogFragment;
import com.aefyr.sai.ui.dialogs.InstallationConfirmationDialogFragment;
import com.aefyr.sai.ui.dialogs.ThemeSelectionDialogFragment;
import com.aefyr.sai.utils.AlertsUtils;
import com.aefyr.sai.utils.PermissionsUtils;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.utils.Theme;
import com.aefyr.sai.viewmodels.InstallerViewModel;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;

import java.io.File;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProviders;

public class MainActivity extends AppCompatActivity implements FilePickerDialogFragment.OnFilesSelectedListener, InstallationConfirmationDialogFragment.ConfirmationListener {

    private InstallerViewModel mViewModel;
    private Button mButton;
    private ImageButton mButtonSettings;
    private PreferencesHelper mHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Theme.apply(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHelper = PreferencesHelper.getInstance(this);

        mButton = findViewById(R.id.button_install);
        mButtonSettings = findViewById(R.id.ib_settings);

        mViewModel = ViewModelProviders.of(this).get(InstallerViewModel.class);
        mViewModel.getState().observe(this, (state) -> {
            switch (state) {
                case IDLE:
                    mButton.setText(R.string.installer_install_apks);
                    mButton.setEnabled(true);
                    mButtonSettings.setEnabled(true);

                    mButtonSettings.setEnabled(true);
                    mButtonSettings.animate()
                            .alpha(1f)
                            .setDuration(200)
                            .start();
                    break;
                case INSTALLING:
                    mButton.setText(R.string.installer_installation_in_progress);
                    mButton.setEnabled(false);

                    mButtonSettings.setEnabled(false);
                    mButtonSettings.animate()
                            .alpha(0f)
                            .setDuration(200)
                            .start();
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
                    AlertsUtils.showAlert(this, getString(R.string.installer_installation_failed), eventData[1]);
                    break;
            }
        });

        mButton.setOnClickListener((v) -> checkPermissionsAndPickFiles());
        findViewById(R.id.button_help).setOnClickListener((v) -> AlertsUtils.showAlert(this, R.string.help, R.string.installer_help));
        findViewById(R.id.ib_toggle_theme).setOnClickListener((v -> new ThemeSelectionDialogFragment().show(getSupportFragmentManager(), "theme_selection_dialog")));
        findViewById(R.id.ib_settings).setOnClickListener((v) -> startActivity(new Intent(MainActivity.this, PreferencesActivity.class)));

        Intent intent = getIntent();
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            //TODO should cancel previous dialog if it exists
            InstallationConfirmationDialogFragment.newInstance(intent.getData()).show(getSupportFragmentManager(), "installation_confirmation_dialog");
            getIntent().setData(null);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (Intent.ACTION_VIEW.equals(intent.getAction()) && intent.getData() != null) {
            InstallationConfirmationDialogFragment.newInstance(intent.getData()).show(getSupportFragmentManager(), "installation_confirmation_dialog");
        }
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

        FilePickerDialogFragment.newInstance(null, getString(R.string.installer_pick_apks), properties).show(getSupportFragmentManager(), "dialog_files_picker");
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
        AppInstalledDialogFragment.newInstance(packageName).show(getSupportFragmentManager(), "dialog_app_installed");
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
}
