package com.aefyr.sai.backup2.impl.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.aefyr.sai.backup.BackupUtils;
import com.aefyr.sai.backup2.backuptask.config.SingleBackupTaskConfig;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.utils.PreferencesKeys;
import com.aefyr.sai.utils.Utils;
import com.aefyr.sai.utils.saf.SafUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class LocalBackupStorage extends ApksBackupStorage implements SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String TAG = "LocalBackupStorage";

    public static final String STORAGE_ID = "local";

    private static LocalBackupStorage sInstance;

    private Context mContext;
    private PreferencesHelper mPrefsHelper;

    public static synchronized LocalBackupStorage getInstance(Context context) {
        return sInstance != null ? sInstance : new LocalBackupStorage(context);
    }

    private LocalBackupStorage(Context context) {
        super();

        mContext = context.getApplicationContext();
        mPrefsHelper = PreferencesHelper.getInstance(mContext);
        mPrefsHelper.getPrefs().registerOnSharedPreferenceChangeListener(this);

        sInstance = this;
    }

    @Override
    public String getStorageId() {
        return STORAGE_ID;
    }

    @Override
    protected Context getContext() {
        return mContext;
    }

    @Override
    protected Uri createFileForTask(SingleBackupTaskConfig config) throws Exception {
        Uri backupFileUri = BackupUtils.createBackupFile(mContext, mPrefsHelper.getBackupDirUri(), config.packageMeta(), config.packApksIntoAnArchive());
        if (backupFileUri == null) {
            throw new Exception("Unable to create backup file");
        }

        return backupFileUri;
    }

    @Override
    protected OutputStream openFileOutputStream(Uri uri) throws Exception {
        return mContext.getContentResolver().openOutputStream(uri);
    }

    @Override
    protected InputStream openFileInputStream(Uri uri) throws Exception {
        return mContext.getContentResolver().openInputStream(uri);
    }

    @Override
    protected void deleteFile(Uri uri) {
        DocumentFile docFile = SafUtils.docFileFromSingleUriOrFileUri(mContext, uri);
        if (docFile != null)
            docFile.delete();
    }

    @Override
    public List<Uri> listBackupFiles() {
        List<Uri> uris = new ArrayList<>();

        DocumentFile backupsDir = SafUtils.docFileFromTreeUriOrFileUri(mContext, mPrefsHelper.getBackupDirUri());
        if (backupsDir == null)
            return uris;

        for (DocumentFile docFile : backupsDir.listFiles()) {
            String docName = docFile.getName();
            if (docName != null && !Utils.getExtension(docName).toLowerCase().equals("apks"))
                continue;

            uris.add(docFile.getUri());
        }

        return uris;
    }

    @Override
    public String getBackupFileHash(Uri uri) {
        DocumentFile docFile = SafUtils.docFileFromSingleUriOrFileUri(mContext, uri);
        if (docFile == null)
            throw new RuntimeException("wtf, doc file is null for uri " + uri);

        //Low budget hash
        return docFile.lastModified() + "/" + docFile.length();
    }

    @Override
    public void deleteBackup(Uri backupUri) {
        DocumentFile docFile = SafUtils.docFileFromSingleUriOrFileUri(mContext, backupUri);
        if (docFile == null)
            return;

        if (docFile.delete()) {
            notifyBackupRemoved(backupUri);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PreferencesKeys.BACKUP_DIR))
            notifyStorageChanged();
    }
}
