package com.aefyr.sai;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;

import com.aefyr.sai.ui.dialogs.AppInstalledDialogFragment;
import com.aefyr.sai.ui.dialogs.FilePickerDialogFragment;
import com.aefyr.sai.ui.dialogs.SimpleAlertDialogFragment;
import com.aefyr.sai.utils.Theme;
import com.aefyr.sai.viewmodels.InstallerViewModel;

import java.io.File;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProviders;

public class MainActivity extends AppCompatActivity implements FilePickerDialogFragment.OnFilesSelectedListener {
    private static final int CODE_REQUEST_PERMISSIONS = 322;

    private InstallerViewModel mViewModel;
    private Button mButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Theme.getInstance(this).isDark())
            setTheme(R.style.AppTheme_Dark);

        setContentView(R.layout.activity_main);

        mButton = findViewById(R.id.button_install);

        mViewModel = ViewModelProviders.of(this).get(InstallerViewModel.class);
        mViewModel.getState().observe(this, (state) -> {
            switch (state) {
                case IDLE:
                    mButton.setText(R.string.installer_pick_apks);
                    mButton.setEnabled(true);
                    break;
                case INSTALLING:
                    mButton.setText(R.string.installer_installation_in_progress);
                    mButton.setEnabled(false);
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
                    alert(R.string.app_name, R.string.installer_installation_failed);
                    break;
            }
        });

        mButton.setOnClickListener((v) -> checkPermissionsAndPickFiles());
        findViewById(R.id.button_help).setOnClickListener((v) -> alert(R.string.help, R.string.installer_help));
        findViewById(R.id.ib_toggle_theme).setOnClickListener((v -> {
            Theme.getInstance(this).setDark(!Theme.getInstance(this).isDark());
            startActivity(new Intent(this, MainActivity.class));
            finish();
            overridePendingTransition(R.anim.alpha_in, R.anim.alpha_out);
        }));
    }

    private void checkPermissionsAndPickFiles() {
        if (Build.VERSION.SDK_INT >= 23 && (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CODE_REQUEST_PERMISSIONS);
            return;
        }

        new FilePickerDialogFragment().show(getSupportFragmentManager(), "dialog_files_picker");
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CODE_REQUEST_PERMISSIONS)
            checkPermissionsAndPickFiles();
        else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void alert(@StringRes int title, @StringRes int message) {
        SimpleAlertDialogFragment.newInstance(getString(title), getString(message)).show(getSupportFragmentManager(), "dialog_alert");
    }

    private void showPackageInstalledAlert(String packageName) {
        AppInstalledDialogFragment.newInstance(packageName).show(getSupportFragmentManager(), "dialog_app_installed");
    }

    @Override
    public void onFilesSelected(List<File> files) {
        mViewModel.installPackages(files);
    }
}
