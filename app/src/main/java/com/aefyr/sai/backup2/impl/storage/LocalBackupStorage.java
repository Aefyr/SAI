package com.aefyr.sai.backup2.impl.storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import com.aefyr.sai.BuildConfig;
import com.aefyr.sai.backup.BackupUtils;
import com.aefyr.sai.backup2.backuptask.config.SingleBackupTaskConfig;
import com.aefyr.sai.installer.ApkSourceBuilder;
import com.aefyr.sai.model.apksource.ApkSource;
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

    public static final String STORAGE_ID = BuildConfig.APPLICATION_ID + ".local_storage";

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

        return namespaceUri(backupFileUri);
    }

    @Override
    protected OutputStream openFileOutputStream(Uri uri) throws Exception {
        return mContext.getContentResolver().openOutputStream(deNamespaceUri(uri));
    }

    @Override
    protected InputStream openFileInputStream(Uri uri) throws Exception {
        return mContext.getContentResolver().openInputStream(deNamespaceUri(uri));
    }

    @Override
    protected void deleteFile(Uri uri) {
        uri = deNamespaceUri(uri);
        DocumentFile docFile = SafUtils.docFileFromSingleUriOrFileUri(mContext, uri);
        if (docFile != null)
            docFile.delete();
    }

    @Override
    protected long getFileSize(Uri uri) {
        uri = deNamespaceUri(uri);
        DocumentFile docFile = SafUtils.docFileFromSingleUriOrFileUri(mContext, uri);
        if (docFile != null)
            return docFile.length();

        return -1;
    }

    @Override
    public List<Uri> listBackupFiles() {
        List<Uri> uris = new ArrayList<>();

        DocumentFile backupsDir = SafUtils.docFileFromTreeUriOrFileUri(mContext, mPrefsHelper.getBackupDirUri());
        if (backupsDir == null)
            return uris;

        for (DocumentFile docFile : backupsDir.listFiles()) {
            if (docFile.isDirectory())
                continue;

            String docName = docFile.getName();
            String docExt = Utils.getExtension(docName);
            if (docExt == null || !docExt.toLowerCase().equals("apks"))
                continue;

            uris.add(namespaceUri(docFile.getUri()));
        }

        return uris;
    }

    @Override
    public String getBackupFileHash(Uri uri) {
        uri = deNamespaceUri(uri);
        DocumentFile docFile = SafUtils.docFileFromSingleUriOrFileUri(mContext, uri);
        if (docFile == null)
            throw new RuntimeException("wtf, doc file is null for uri " + uri);

        //Low budget hash
        return docFile.lastModified() + "/" + docFile.length();
    }

    @Override
    public void deleteBackup(Uri backupUri) {
        backupUri = deNamespaceUri(backupUri);
        DocumentFile docFile = SafUtils.docFileFromSingleUriOrFileUri(mContext, backupUri);
        if (docFile == null)
            return;

        if (docFile.delete()) {
            notifyBackupRemoved(namespaceUri(backupUri));
        }
    }

    @Override
    public ApkSource createApkSource(Uri backupUri) {
        return new ApkSourceBuilder(mContext)
                .fromZipContentUri(deNamespaceUri(backupUri))
                .build();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(PreferencesKeys.BACKUP_DIR))
            notifyStorageChanged();
    }

    private Uri deNamespaceUri(Uri namespacedUri) {
        if (!getStorageId().equals(namespacedUri.getAuthority()))
            throw new IllegalArgumentException("Passed uri doesn't belong to this storage");

        return Uri.parse(namespacedUri.getQueryParameter("uri"));
    }

    private Uri namespaceUri(Uri uri) {
        return new Uri.Builder()
                .scheme("sbs")
                .authority(getStorageId())
                .appendQueryParameter("uri", uri.toString())
                .build();
    }
}
