package com.aefyr.sai.ui.dialogs;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.aefyr.sai.R;
import com.aefyr.sai.backup.BackupService;
import com.aefyr.sai.model.backup.PackageMeta;

import java.io.File;

public class BackupDialogFragment extends DialogFragment {
    private static final String ARG_PACKAGE = "package";

    private PackageMeta mPackage;

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

        Bundle args = getArguments();
        if (args == null)
            return;
        mPackage = args.getParcelable(ARG_PACKAGE);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getContext())
                .setMessage(getString(R.string.backup_backup_prompt, mPackage.label))
                .setPositiveButton(R.string.yes, (d, w) -> {
                    BackupService.enqueueBackup(getContext(), mPackage, Uri.fromFile(generateBackupFilePath()));
                    dismiss();
                })
                .setNegativeButton(R.string.cancel, (d, w) -> dismiss())
                .create();
    }

    private File generateBackupFilePath() {
        File backupsDir = new File(Environment.getExternalStorageDirectory(), "SAI");
        backupsDir.mkdir();
        return new File(backupsDir, String.format("%s-%d.apks", mPackage.packageName, System.currentTimeMillis()));
    }
}
