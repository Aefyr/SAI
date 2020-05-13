package com.aefyr.sai.ui.dialogs;

import android.app.Dialog;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.aefyr.sai.R;
import com.aefyr.sai.backup2.impl.DefaultBackupManager;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DeleteBackupConfirmationDialog extends DialogFragment {

    private static final String ARG_STORAGE_ID = "storage_id";
    private static final String ARG_BACKUP_URI = "backup_uri";
    private static final String ARG_TIMESTAMP = "timestamp";

    private String mStorageId;
    private Uri mBackupUri;
    private long mTimestamp;

    private SimpleDateFormat mBackupTimeSdf = new SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault());

    public static DeleteBackupConfirmationDialog newInstance(String storageId, Uri backupUri, long timestamp) {

        Bundle args = new Bundle();
        args.putString(ARG_STORAGE_ID, storageId);
        args.putParcelable(ARG_BACKUP_URI, backupUri);
        args.putLong(ARG_TIMESTAMP, timestamp);

        DeleteBackupConfirmationDialog dialog = new DeleteBackupConfirmationDialog();
        dialog.setArguments(args);

        return dialog;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = requireArguments();
        mStorageId = args.getString(ARG_STORAGE_ID);
        mBackupUri = args.getParcelable(ARG_BACKUP_URI);
        mTimestamp = args.getLong(ARG_TIMESTAMP);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(requireContext())
                .setMessage(getString(R.string.backup_delete_backup_prompt, mBackupTimeSdf.format(new Date(mTimestamp))))
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    DefaultBackupManager.getInstance(requireContext()).deleteBackup(mStorageId, mBackupUri, null, null);
                }).create();
    }
}
