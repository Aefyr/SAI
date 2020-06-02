package com.aefyr.sai.backup2.impl.local;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

import androidx.documentfile.provider.DocumentFile;

import com.aefyr.sai.backup2.backuptask.config.SingleBackupTaskConfig;
import com.aefyr.sai.backup2.impl.storage.ApksBackupStorage;
import com.aefyr.sai.installer.ApkSourceBuilder;
import com.aefyr.sai.model.apksource.ApkSource;
import com.aefyr.sai.utils.Utils;
import com.aefyr.sai.utils.saf.SafUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class LocalBackupStorage extends ApksBackupStorage implements LocalBackupStorageProvider.OnConfigChangeListener {
    private static final String TAG = "LocalBackupStorage";

    private LocalBackupStorageProvider mProvider;

    private Context mContext;

    private HandlerThread mWorkerHandlerThread;
    private Handler mWorkerHandler;

    LocalBackupStorage(LocalBackupStorageProvider provider, Context context) {
        super();

        mProvider = provider;
        mContext = context.getApplicationContext();

        mWorkerHandlerThread = new HandlerThread("LocalBackupStorage.Worker");
        mWorkerHandlerThread.start();
        mWorkerHandler = new Handler(mWorkerHandlerThread.getLooper());

        mProvider.addOnConfigChangeListener(this, mWorkerHandler);
    }

    @Override
    public String getStorageId() {
        return mProvider.getId();
    }

    @Override
    protected Context getContext() {
        return mContext;
    }

    @Override
    protected Uri createFileForTask(SingleBackupTaskConfig config) throws Exception {
        Uri backupFileUri = LocalBackupUtils.createBackupFile(mContext, getBackupDirUriOrThrow(), config.packageMeta(), true);
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

        DocumentFile backupsDir = SafUtils.docFileFromTreeUriOrFileUri(mContext, getBackupDirUriOrThrow());
        if (backupsDir == null)
            return uris;

        for (DocumentFile docFile : backupsDir.listFiles()) {
            if (docFile.isDirectory())
                continue;

            String docName = docFile.getName();
            if (docName == null)
                continue;

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

        if (!docFile.exists()) {
            notifyBackupRemoved(namespaceUri(backupUri));
        } else if (docFile.delete()) {
            notifyBackupRemoved(namespaceUri(backupUri));
        }
    }

    @Override
    public ApkSource createApkSource(Uri backupUri) {
        return new ApkSourceBuilder(mContext)
                .fromZipContentUri(deNamespaceUri(backupUri))
                .build();
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

    private Uri getBackupDirUriOrThrow() throws IllegalStateException {
        Uri backupDirUri = mProvider.getBackupDirUri();
        if (backupDirUri == null) {
            throw new IllegalStateException("Backup dir uri is null, have you set up LocalBackupStorageProvider?");
        }

        return backupDirUri;
    }

    @Override
    public void onBackupDirChanged() {
        notifyStorageChanged();
    }
}
