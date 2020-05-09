package com.aefyr.sai.backup2.impl.storage;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Handler;
import android.util.Log;

import com.aefyr.sai.backup2.BackupTaskConfig;
import com.aefyr.sai.model.backup.SaiExportedAppMeta;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ApksBackupTaskExecutor {
    private static final String TAG = "ApksBackupTaskExecutor";

    private Context mContext;
    private BackupTaskConfig mConfig;
    private DelegatedFile mDelegatedFile;

    private Listener mListener;
    private Handler mListenerHandler;

    private AtomicBoolean mIsStarted = new AtomicBoolean(false);
    private AtomicBoolean mIsCancelled = new AtomicBoolean(false);

    public ApksBackupTaskExecutor(Context context, BackupTaskConfig config, DelegatedFile delegatedFile) {
        mContext = context.getApplicationContext();
        mConfig = config;
        mDelegatedFile = delegatedFile;
    }

    public void setListener(Listener listener, Handler listenerHandler) {
        ensureNotStarted();

        mListener = listener;
        mListenerHandler = listenerHandler;
    }

    public boolean isStarted() {
        return mIsStarted.get();
    }

    public boolean isCancelled() {
        return mIsCancelled.get();
    }

    public void requestCancellation() {
        mIsCancelled.set(true);
    }

    public void execute(Executor executor) {
        if (mIsStarted.getAndSet(true)) {
            throw new IllegalStateException("Unable to call this method after execution has been started");
        }

        executor.execute(this::executeInternal);
    }

    private void executeInternal() {
        try {
            notifyStarted();

            ensureNotCancelled();

            List<File> apkFiles;
            if (mConfig.apksToBackup().size() == 0)
                apkFiles = getAllApkFilesForPackage(mConfig.packageMeta().packageName);
            else
                apkFiles = mConfig.apksToBackup();

            if (!mConfig.packApksIntoAnArchive() && apkFiles.size() != 1)
                throw new IllegalArgumentException("No packing requested but multiple APKs are to be exported");

            if (!mConfig.packApksIntoAnArchive())
                executeBackupWithoutPacking(mConfig, apkFiles.get(0));
            else
                executeBackupWithPacking(mConfig, apkFiles);

            notifySucceeded();
        } catch (TaskCancelledException e) {
            mDelegatedFile.delete();
            notifyCancelled();
        } catch (Exception e) {
            mDelegatedFile.delete();
            notifyFailed(e);
        }
    }

    private void executeBackupWithPacking(BackupTaskConfig config, List<File> apkFiles) throws Exception {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(mDelegatedFile.openOutputStream())) {

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
                    iconFile = Utils.saveImageFromUriAsPng(mContext, config.packageMeta().iconUri);
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

    private void executeBackupWithoutPacking(BackupTaskConfig config, File apkFile) throws Exception {
        try (FileInputStream apkInputStream = new FileInputStream(apkFile); OutputStream outputStream = mDelegatedFile.openOutputStream()) {
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

    private void ensureNotStarted() {
        if (mIsStarted.get())
            throw new IllegalStateException("Unable to call this method after execution has been started");
    }

    private void ensureNotCancelled() throws TaskCancelledException {
        if (isCancelled())
            throw new TaskCancelledException();
    }

    private List<File> getAllApkFilesForPackage(String pkg) throws Exception {
        ApplicationInfo applicationInfo = mContext.getPackageManager().getApplicationInfo(pkg, 0);

        List<File> apkFiles = new ArrayList<>();
        apkFiles.add(new File(applicationInfo.publicSourceDir));

        if (applicationInfo.splitPublicSourceDirs != null) {
            for (String splitPath : applicationInfo.splitPublicSourceDirs)
                apkFiles.add(new File(splitPath));
        }

        return apkFiles;
    }

    private void notifyStarted() {
        if (mListener != null)
            mListenerHandler.post(() -> mListener.onStart());
    }

    private void notifyProgressChanged(long current, long goal) {
        if (mListener != null)
            mListenerHandler.post(() -> mListener.onProgressChanged(current, goal));
    }

    private void notifyCancelled() {
        if (mListener != null)
            mListenerHandler.post(() -> mListener.onCancelled());
    }

    private void notifySucceeded() {
        if (mListener != null)
            mListenerHandler.post(() -> mListener.onSuccess());
    }

    private void notifyFailed(Exception e) {
        if (mListener != null)
            mListenerHandler.post(() -> mListener.onError(e));
    }


    public interface DelegatedFile {

        OutputStream openOutputStream() throws Exception;

        void delete();

    }

    public interface Listener {

        void onStart();

        void onProgressChanged(long current, long goal);

        void onCancelled();

        void onSuccess();

        void onError(Exception e);

    }

    private static class TaskCancelledException extends Exception {

    }

}
