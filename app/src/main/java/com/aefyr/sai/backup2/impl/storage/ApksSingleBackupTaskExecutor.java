package com.aefyr.sai.backup2.impl.storage;

import android.content.Context;
import android.util.Log;

import com.aefyr.sai.backup2.backuptask.config.SingleBackupTaskConfig;
import com.aefyr.sai.backup2.backuptask.executor.SingleBackupTaskExecutor;
import com.aefyr.sai.model.backup.SaiExportedAppMeta;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ApksSingleBackupTaskExecutor extends SingleBackupTaskExecutor {
    private static final String TAG = "ApksBackupTaskExecutor";


    public ApksSingleBackupTaskExecutor(Context context, SingleBackupTaskConfig config, DelegatedFile delegatedFile) {
        super(context, config, delegatedFile);
    }

    @Override
    protected void executeInternal() {
        try {
            notifyStarted();

            ensureNotCancelled();

            List<File> apkFiles;
            if (getConfig().apksToBackup().size() == 0)
                apkFiles = getAllApkFilesForPackage(getConfig().packageMeta().packageName);
            else
                apkFiles = getConfig().apksToBackup();

            if (!getConfig().packApksIntoAnArchive() && apkFiles.size() != 1)
                throw new IllegalArgumentException("No packing requested but multiple APKs are to be exported");

            if (!getConfig().packApksIntoAnArchive())
                executeBackupWithoutPacking(getConfig(), apkFiles.get(0));
            else
                executeBackupWithPacking(getConfig(), apkFiles);

            notifySucceeded(getFile().readMeta());
        } catch (TaskCancelledException e) {
            getFile().delete();
            notifyCancelled();
        } catch (Exception e) {
            getFile().delete();
            notifyFailed(e);
        }
    }

    private void executeBackupWithPacking(SingleBackupTaskConfig config, List<File> apkFiles) throws Exception {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(getFile().openOutputStream())) {

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
                        ensureNotCancelled();

                        zipOutputStream.write(buffer, 0, read);
                        currentProgress += read;
                        notifyProgressChanged(currentProgress, maxProgress);
                    }
                }
                zipOutputStream.closeEntry();
            }
        }
    }

    private void executeBackupWithoutPacking(SingleBackupTaskConfig config, File apkFile) throws Exception {
        try (FileInputStream apkInputStream = new FileInputStream(apkFile); OutputStream outputStream = getFile().openOutputStream()) {
            if (outputStream == null)
                throw new IOException("Unable to open output stream");

            long currentProgress = 0;
            long maxProgress = apkFile.length();

            byte[] buf = new byte[1024 * 512];
            int read;
            while ((read = apkInputStream.read(buf)) > 0) {
                ensureNotCancelled();

                outputStream.write(buf, 0, read);
                currentProgress += read;
                notifyProgressChanged(currentProgress, maxProgress);
            }
        }
    }

}
