package com.aefyr.sai.ui.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.ImageButton;

import com.aefyr.sai.R;
import com.aefyr.sai.ui.dialogs.AppInstalledDialogFragment;
import com.aefyr.sai.ui.dialogs.FilePickerDialogFragment;
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

public class MainActivity extends AppCompatActivity implements FilePickerDialogFragment.OnFilesSelectedListener {
    private InstallerViewModel mViewModel;
    private Button mButton;
    private ImageButton mButtonSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Theme.getInstance(this).apply(this);

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton = findViewById(R.id.button_install);
        mButtonSettings = findViewById(R.id.ib_settings);

        mViewModel = ViewModelProviders.of(this).get(InstallerViewModel.class);
        mViewModel.getState().observe(this, (state) -> {
            switch (state) {
                case IDLE:
                    mButton.setText(R.string.installer_pick_apks);
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
        findViewById(R.id.ib_toggle_theme).setOnClickListener((v -> {
            Theme.getInstance(this).setDark(!Theme.getInstance(this).isDark());
            startActivity(new Intent(this, MainActivity.class));
            finish();
            overridePendingTransition(R.anim.alpha_in, R.anim.alpha_out);
        }));
        findViewById(R.id.ib_settings).setOnClickListener((v) -> startActivity(new Intent(MainActivity.this, PreferencesActivity.class)));
    }

    private void checkPermissionsAndPickFiles() {
        if (!PermissionsUtils.checkAndRequestStoragePermissions(this))
            return;

        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.MULTI_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = Environment.getExternalStorageDirectory();
        properties.offset = new File(PreferencesHelper.getInstance(this).getHomeDirectory());
        properties.extensions = new String[]{"apk"};

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
        mViewModel.installPackages(files);
    }
}
