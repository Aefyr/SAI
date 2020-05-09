package com.aefyr.sai.backup2.impl;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.util.Log;

import com.aefyr.sai.backup2.BackupFileMeta;
import com.aefyr.sai.backup2.BackupTaskConfig;
import com.aefyr.sai.model.backup.SaiExportedAppMeta;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public abstract class ApksBackupStorage extends BaseBackupStorage {
    private static final String TAG = "ApksBackupStorage";
    private static Uri EMPTY_ICON = new Uri.Builder().scheme("no").authority("icon").build();


    private ExecutorService mExecutor = Executors.newFixedThreadPool(4);

    protected abstract Context getContext();

    protected abstract Uri createFileForTask(BackupTaskConfig config) throws Exception;

    protected abstract OutputStream openFileOutputStream(Uri uri) throws Exception;

    protected abstract InputStream openFileInputStream(Uri uri) throws Exception;

    protected abstract void deleteFile(Uri uri);

    @Override
    public List<Uri> listBackupFiles() {
        return null;
    }

    @Override
    public String getBackupFileHash(Uri uri) {
        return null;
    }

    @Override
    public BackupFileMeta getMetaForBackupFile(Uri uri) throws Exception {

        BackupFileMeta backupFileMeta = null;
        Uri iconUri = EMPTY_ICON;
        try (ZipInputStream zipInputStream = new ZipInputStream(openFileInputStream(uri))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.getName().equals(SaiExportedAppMeta.META_FILE)) {
                    SaiExportedAppMeta appMeta = SaiExportedAppMeta.deserialize(IOUtils.readStreamNoClose(zipInputStream));
                    backupFileMeta = new BackupFileMeta();
                    backupFileMeta.uri = uri;
                    backupFileMeta.contentHash = getBackupFileHash(uri);

                    backupFileMeta.pkg = appMeta.packageName();
                    backupFileMeta.label = appMeta.label();
                    backupFileMeta.versionCode = appMeta.versionCode();
                    backupFileMeta.versionName = appMeta.versionName();
                    backupFileMeta.exportTimestamp = appMeta.exportTime();
                    backupFileMeta.storageId = getStorageId();
                } else if (zipEntry.getName().equals(SaiExportedAppMeta.ICON_FILE)) {
                    File iconFile = Utils.createUniqueFileInDirectory(new File(getContext().getFilesDir(), "BackupStorageIcons"), "png");
                    if (iconFile == null)
                        continue;

                    try (OutputStream out = new FileOutputStream(iconFile)) {
                        IOUtils.copyStream(zipInputStream, out);
                        iconUri = Uri.fromFile(iconFile);
                    } catch (IOException e) {
                        Log.w(TAG, "Unable to extract icon", e);
                    }
                }

                if (backupFileMeta != null && !EMPTY_ICON.equals(iconUri))
                    break;
            }
        }

        if (backupFileMeta == null)
            throw new Exception("Meta file not found in archive");

        backupFileMeta.iconUri = iconUri;

        return backupFileMeta;
    }

    private List<File> getAllApkFilesForPackage(String pkg) throws Exception {
        ApplicationInfo applicationInfo = getContext().getPackageManager().getApplicationInfo(pkg, 0);

        List<File> apkFiles = new ArrayList<>();
        apkFiles.add(new File(applicationInfo.publicSourceDir));

        if (applicationInfo.splitPublicSourceDirs != null) {
            for (String splitPath : applicationInfo.splitPublicSourceDirs)
                apkFiles.add(new File(splitPath));
        }

        return apkFiles;
    }

    @Override
    public void backupApp(BackupTaskConfig config, String tag) {
        mExecutor.execute(() -> {
            try {
                BackupFileMeta meta = executeBackup(config, tag);
                notifyBackupCompleted(tag, meta);
                notifyBackupAdded(meta);
            } catch (Exception e) {
                Log.w(TAG, e);
                notifyBackupFailed(tag, e);
            }
        });
    }

    private BackupFileMeta executeBackup(BackupTaskConfig config, String tag) throws Exception {
        Uri destination = createFileForTask(config);

        try {
            List<File> apkFiles;
            if (config.apksToBackup().size() == 0)
                apkFiles = getAllApkFilesForPackage(config.packageMeta().packageName);
            else
                apkFiles = config.apksToBackup();

            if (!config.packApksIntoAnArchive() && apkFiles.size() != 1)
                throw new IllegalArgumentException("No packing requested but multiple APKs are to be exported");

            if (!config.packApksIntoAnArchive())
                executeBackupWithoutPacking(tag, config, apkFiles.get(0), destination);
            else
                executeBackupWithPacking(tag, config, apkFiles, destination);

            return getMetaForBackupFile(destination);
        } catch (Exception e) {
            deleteFile(destination);
            throw e;
        }
    }

    private void executeBackupWithPacking(String tag, BackupTaskConfig config, List<File> apkFiles, Uri destination) throws Exception {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(openFileOutputStream(destination))) {

            long currentProgress = 0;
            long maxProgress = 0;
            for (File apkFile : apkFiles) {
                maxProgress += apkFile.length();
            }

            //Meta
            byte[] meta = SaiExportedAppMeta.fromPackageMeta(config.packageMeta(), System.currentTimeMillis()).serialize();

            zipOutputStream.setMethod(ZipOutputStream.STORED);
            ZipEntry metaZipEntry = new ZipEntry(SaiExportedAppMeta.META_FILE);
            metaZipEntry.setMethod(ZipEntry.STORED);
            metaZipEntry.setCompressedSize(meta.length);
            metaZipEntry.setSize(meta.length);
            metaZipEntry.setCrc(IOUtils.calculateBytesCrc32(meta));

            zipOutputStream.putNextEntry(metaZipEntry);
            zipOutputStream.write(meta);
            zipOutputStream.closeEntry();


            //Icon
            if (config.packageMeta().iconUri != null) {
                File iconFile = null;
                try {
                    iconFile = Utils.saveImageFromUriAsPng(getContext(), config.packageMeta().iconUri);
                } catch (Exception e) {
                    Log.w(TAG, "Unable to save app icon", e);
                }

                if (iconFile != null) {
                    zipOutputStream.setMethod(ZipOutputStream.STORED);

                    ZipEntry zipEntry = new ZipEntry(SaiExportedAppMeta.ICON_FILE);
                    zipEntry.setMethod(ZipEntry.STORED);
                    zipEntry.setCompressedSize(iconFile.length());
                    zipEntry.setSize(iconFile.length());
                    zipEntry.setCrc(IOUtils.calculateFileCrc32(iconFile));

                    zipOutputStream.putNextEntry(zipEntry);

                    try (FileInputStream iconInputStream = new FileInputStream(iconFile)) {
                        IOUtils.copyStream(iconInputStream, zipOutputStream);
                    }

                    zipOutputStream.closeEntry();
                    iconFile.delete();
                }
            }


            //APKs
            for (File apkFile : apkFiles) {
                zipOutputStream.setMethod(ZipOutputStream.STORED);

                ZipEntry zipEntry = new ZipEntry(apkFile.getName());
                zipEntry.setMethod(ZipEntry.STORED);
                zipEntry.setCompressedSize(apkFile.length());
                zipEntry.setSize(apkFile.length());
                zipEntry.setCrc(IOUtils.calculateFileCrc32(apkFile));

                zipOutputStream.putNextEntry(zipEntry);

                try (FileInputStream apkInputStream = new FileInputStream(apkFile)) {
                    byte[] buffer = new byte[1024 * 512];
                    int read;

                    while ((read = apkInputStream.read(buffer)) > 0) {
                        zipOutputStream.write(buffer, 0, read);
                        currentProgress += read;
                        notifyBackupProgressChanged(tag, currentProgress, maxProgress);
                    }
                }
                zipOutputStream.closeEntry();
            }
        }
    }

    private void executeBackupWithoutPacking(String tag, BackupTaskConfig config, File apkFile, Uri destination) throws Exception {
        try (FileInputStream apkInputStream = new FileInputStream(apkFile); OutputStream outputStream = openFileOutputStream(destination)) {
            if (outputStream == null)
                throw new IOException("Unable to open output stream");

            long currentProgress = 0;
            long maxProgress = apkFile.length();

            byte[] buf = new byte[1024 * 1024];
            int read;
            while ((read = apkInputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, read);
                currentProgress += read;
                notifyBackupProgressChanged(tag, currentProgress, maxProgress);
            }
        }
    }
}
