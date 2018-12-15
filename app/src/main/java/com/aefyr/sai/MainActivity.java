package com.aefyr.sai;

import android.Manifest;
import android.app.Dialog;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.Button;

import com.aefyr.sai.viewmodels.InstallerViewModel;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProviders;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "SAI";
    private static final int CODE_REQUEST_PERMISSIONS = 322;

    private InstallerViewModel mViewModel;
    private Button mButton;

    private AlertDialog mAlertDialog;
    private FilePickerDialog mPickerDialog;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
    }

    private void checkPermissionsAndPickFiles() {
        if (Build.VERSION.SDK_INT >= 23 && (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, CODE_REQUEST_PERMISSIONS);
            return;
        }

        DialogProperties properties = new DialogProperties();
        properties.selection_mode = DialogConfigs.MULTI_MODE;
        properties.selection_type = DialogConfigs.FILE_SELECT;
        properties.root = Environment.getExternalStorageDirectory();
        properties.error_dir = new File(DialogConfigs.DEFAULT_DIR);
        properties.offset = Environment.getExternalStorageDirectory();
        properties.extensions = new String[]{"apk"};

        dismissDialogIfNeeded(mPickerDialog);
        mPickerDialog = new FilePickerDialog(MainActivity.this, properties);
        mPickerDialog.setDialogSelectionListener((files) -> {
            ArrayList<File> apkFiles = new ArrayList<>(files.length);

            for (String file : files)
                apkFiles.add(new File(file));

            mViewModel.installPackages(apkFiles);
        });
        mPickerDialog.setTitle(R.string.installer_pick_apks);
        mPickerDialog.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CODE_REQUEST_PERMISSIONS)
            checkPermissionsAndPickFiles();
        else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //To avoid window leaks
        dismissDialogIfNeeded(mPickerDialog);
        dismissDialogIfNeeded(mAlertDialog);
    }

    private void alert(@StringRes int title, @StringRes int message) {
        dismissDialogIfNeeded(mAlertDialog);
        mAlertDialog = new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton(R.string.ok, null).create();
        mAlertDialog.show();
    }

    private void showPackageInstalledAlert(String packageName) {
        String appLabel = null;
        Intent appLaunchIntent = null;

        try {
            PackageManager pm = getPackageManager();
            ApplicationInfo appInfo = pm.getApplicationInfo(packageName, 0);
            appLabel = pm.getApplicationLabel(appInfo).toString();
            appLaunchIntent = pm.getLaunchIntentForPackage(packageName);
            Objects.requireNonNull(appLaunchIntent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        } catch (Exception e) {
            Log.w(TAG, e);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setMessage(appLabel == null? getString(R.string.installer_app_installed):String.format(getString(R.string.installer_app_installed_full), appLabel))
                .setNegativeButton(R.string.ok, null);

        Intent finalAppLaunchIntent = appLaunchIntent;
        if(appLaunchIntent != null)
            builder.setPositiveButton(R.string.installer_open, (d, w) -> startActivity(finalAppLaunchIntent));

        dismissDialogIfNeeded(mAlertDialog);
        mAlertDialog = builder.create();
        mAlertDialog.show();
    }

    private void dismissDialogIfNeeded(Dialog dialog) {
        if (dialog != null && dialog.isShowing())
            dialog.dismiss();
    }
}
