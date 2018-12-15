package com.aefyr.sai;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.widget.Button;
import android.widget.TextView;

import com.aefyr.sai.viewmodels.InstallerViewModel;
import com.github.angads25.filepicker.model.DialogConfigs;
import com.github.angads25.filepicker.model.DialogProperties;
import com.github.angads25.filepicker.view.FilePickerDialog;

import java.io.File;
import java.util.ArrayList;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProviders;

public class MainActivity extends AppCompatActivity {
    private static final int CODE_REQUEST_PERMISSIONS = 322;

    private InstallerViewModel mViewModel;
    private CardView mCardView;
    private TextView mTextView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mTextView = findViewById(R.id.pickapk);

        mCardView = findViewById(R.id.button_install);

        mViewModel = ViewModelProviders.of(this).get(InstallerViewModel.class);
        mViewModel.getInstaller().observe(this, (status -> {
            switch (status) {
                case IDLE:
                    mTextView.setText(R.string.installer_pick_apks);
                    mCardView.setEnabled(true);
                    break;
                case INSTALLING:
                    mTextView.setText(R.string.installer_installation_in_progress);
                    mCardView.setEnabled(false);
                    break;
                case INSTALLED:
                    alert(R.string.app_name, R.string.installer_app_installed);
                    mViewModel.resetInstaller();
                    break;
                case FAILED:
                    alert(R.string.error, R.string.installer_installation_failed);
                    mViewModel.resetInstaller();
                    break;
            }
        }));

        mCardView.setOnClickListener((v) -> checkPermissionsAndPickFiles());
        findViewById(R.id.button_help).setOnClickListener((v)-> alert(R.string.help, R.string.installer_help));
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

        FilePickerDialog dialog = new FilePickerDialog(MainActivity.this, properties);
        dialog.setDialogSelectionListener((files) -> {
            ArrayList<File> apkFiles = new ArrayList<>(files.length);

            for (String file : files)
                apkFiles.add(new File(file));

            mViewModel.installPackages(apkFiles);
        });
        dialog.setTitle(R.string.installer_pick_apks);
        dialog.show();
    }

    private void alert(@StringRes int title, @StringRes int message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message).setPositiveButton(R.string.ok, null).create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == CODE_REQUEST_PERMISSIONS)
            checkPermissionsAndPickFiles();
        else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}
