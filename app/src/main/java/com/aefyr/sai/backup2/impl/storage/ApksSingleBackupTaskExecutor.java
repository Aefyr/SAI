package com.aefyr.sai.backup2.impl.storage;

import android.content.Context;
import android.util.Log;

import com.aefyr.sai.backup2.backuptask.config.SingleBackupTaskConfig;
import com.aefyr.sai.backup2.backuptask.executor.SingleBackupTaskExecutor;
import com.aefyr.sai.backup2.impl.components.StandardComponentTypes;
import com.aefyr.sai.model.backup.SaiExportedAppMeta;
import com.aefyr.sai.model.backup.SaiExportedAppMeta2;
import com.aefyr.sai.utils.DbgPreferencesHelper;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ApksSingleBackupTaskExecutor extends SingleBackupTaskExecutor {
    private static final String TAG = "ApksBackupTaskExecutor";
    private static final int BUFFER_SIZE = 1024 * 512;


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

            if (getConfig().exportMode()) {
                if (apkFiles.size() == 1) {
                    executeWithoutPacking(apkFiles.get(0));
                    notifySucceeded(null);
                } else {
                    throw new IllegalArgumentException("Config has exportMode set to true, but there are multiple apk files");
                }
            } else {
                executeBackupWithPacking(getConfig(), apkFiles);
                notifySucceeded(getFile().readMeta());
            }
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

            apkFiles = new ArrayList<>(apkFiles);
            Collections.sort(apkFiles);

            long timestamp = DbgPreferencesHelper.getInstance(getContext()).addFakeTimestampToBackups() ? 946684800000L : System.currentTimeMillis();

            long currentProgress = 0;
            long totalApkBytesCount = 0;
            for (File apkFile : apkFiles) {
                totalApkBytesCount += apkFile.length();
            }

            //Meta v2
            byte[] metaV2 = SaiExportedAppMeta2.createForPackage(getContext(), config.packageMeta().packageName, timestamp)
                    .addBackupComponent(StandardComponentTypes.TYPE_APK_FILES, totalApkBytesCount)
                    .serialize();

            zipOutputStream.setMethod(ZipOutputStream.STORED);
            ZipEntry metaV2ZipEntry = new ZipEntry(SaiExportedAppMeta2.META_FILE);
            metaV2ZipEntry.setMethod(ZipEntry.STORED);
            metaV2ZipEntry.setCompressedSize(metaV2.length);
            metaV2ZipEntry.setSize(metaV2.length);
            metaV2ZipEntry.setCrc(IOUtils.calculateBytesCrc32(metaV2));
            metaV2ZipEntry.setTime(timestamp);

            zipOutputStream.putNextEntry(metaV2ZipEntry);
            zipOutputStream.write(metaV2);
            zipOutputStream.closeEntry();

            //Meta v1
            byte[] metaV1 = SaiExportedAppMeta.fromPackageMeta(config.packageMeta(), timestamp).serialize();

            zipOutputStream.setMethod(ZipOutputStream.STORED);
            ZipEntry metaV1ZipEntry = new ZipEntry(SaiExportedAppMeta.META_FILE);
            metaV1ZipEntry.setMethod(ZipEntry.STORED);
            metaV1ZipEntry.setCompressedSize(metaV1.length);
            metaV1ZipEntry.setSize(metaV1.length);
            metaV1ZipEntry.setCrc(IOUtils.calculateBytesCrc32(metaV1));
            metaV1ZipEntry.setTime(timestamp);

            zipOutputStream.putNextEntry(metaV1ZipEntry);
            zipOutputStream.write(metaV1);
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
                    zipEntry.setTime(timestamp);

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
                zipEntry.setTime(timestamp);

                zipOutputStream.putNextEntry(zipEntry);

                try (FileInputStream apkInputStream = new FileInputStream(apkFile)) {
                    byte[] buffer = new byte[BUFFER_SIZE];
                    int read;

                    while ((read = apkInputStream.read(buffer)) > 0) {
                        ensureNotCancelled();

                        zipOutputStream.write(buffer, 0, read);
                        currentProgress += read;
                        notifyProgressChanged(currentProgress, totalApkBytesCount);
                    }
                }
                zipOutputStream.closeEntry();
            }
        }
    }

    private void executeWithoutPacking(File apkFile) throws Exception {
        try (FileInputStream apkInputStream = new FileInputStream(apkFile); OutputStream outputStream = getFile().openOutputStream()) {
            long currentProgress = 0;
            long maxProgress = apkFile.length();

            byte[] buf = new byte[BUFFER_SIZE];
            int read;
            while ((read = apkInputStream.read(buf)) > 0) {
                outputStream.write(buf, 0, read);
                currentProgress += read;
                notifyProgressChanged(currentProgress, maxProgress);
            }
        }
    }
}
