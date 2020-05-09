package com.aefyr.sai.backup2.impl.storage;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;

import com.aefyr.sai.backup2.BackupFileMeta;
import com.aefyr.sai.backup2.BackupTaskConfig;
import com.aefyr.sai.model.backup.SaiExportedAppMeta;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class ApksBackupStorage extends BaseBackupStorage {
    private static final String TAG = "ApksBackupStorage";
    private static Uri EMPTY_ICON = new Uri.Builder().scheme("no").authority("icon").build();


    private ExecutorService mTaskExecutor = Executors.newFixedThreadPool(4);
    private ExecutorService mFinisherExecutor = Executors.newFixedThreadPool(4);

    @GuardedBy("mTasks")
    private final Map<String, BackupTaskConfig> mTasks = new HashMap<>();

    @GuardedBy("mTaskExecutors")
    private final Map<String, ApksBackupTaskExecutor> mTaskExecutors = new HashMap<>();

    private HandlerThread mTaskProgressHandlerThread;
    private Handler mTaskProgressHandler;

    protected ApksBackupStorage() {
        mTaskProgressHandlerThread = new HandlerThread("ApksBackupStorage.TaskProgress");
        mTaskProgressHandlerThread.start();
        mTaskProgressHandler = new Handler(mTaskProgressHandlerThread.getLooper());
    }

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

    @Override
    public String createBackupTask(BackupTaskConfig config) {
        String token = UUID.randomUUID().toString();

        synchronized (mTasks) {
            mTasks.put(token, config);
        }

        notifyBackupTaskStatusChanged(BackupTaskStatus.created(token, config));

        return token;
    }

    @Override
    public void startBackupTask(String taskToken) {
        BackupTaskConfig config;
        synchronized (mTasks) {
            config = mTasks.remove(taskToken);
        }
        if (config == null)
            return;

        InternalDelegatedFile delegatedFile = new InternalDelegatedFile(config);
        ApksBackupTaskExecutor taskExecutor = new ApksBackupTaskExecutor(getContext(), config, delegatedFile);
        taskExecutor.setListener(new ApksBackupTaskExecutor.Listener() {
            @Override
            public void onStart() {
                notifyBackupTaskStatusChanged(BackupTaskStatus.inProgress(taskToken, config, 0, 1));
            }

            @Override
            public void onProgressChanged(long current, long goal) {
                notifyBackupTaskStatusChanged(BackupTaskStatus.inProgress(taskToken, config, current, goal));
            }

            @Override
            public void onCancelled() {
                notifyBackupTaskStatusChanged(BackupTaskStatus.cancelled(taskToken, config));
            }

            @Override
            public void onSuccess() {
                mFinisherExecutor.execute(() -> readMetaAndFinishBackupTask(taskToken, config, delegatedFile));
            }

            @Override
            public void onError(Exception e) {
                Log.w(TAG, String.format("Unable to export %s, task token is %s", config.packageMeta().packageName, taskToken), e);
                notifyBackupTaskStatusChanged(BackupTaskStatus.failed(taskToken, config, e));
            }
        }, mTaskProgressHandler);
        synchronized (mTaskExecutors) {
            mTaskExecutors.put(taskToken, taskExecutor);
        }
        notifyBackupTaskStatusChanged(BackupTaskStatus.queued(taskToken, config));
        taskExecutor.execute(mTaskExecutor);
    }

    @Override
    public void cancelBackupTask(String taskToken) {
        synchronized (mTaskExecutors) {
            ApksBackupTaskExecutor taskExecutor = mTaskExecutors.get(taskToken);
            if (taskExecutor != null)
                taskExecutor.requestCancellation();
        }
    }

    private void readMetaAndFinishBackupTask(String taskToken, BackupTaskConfig config, InternalDelegatedFile delegatedFile) {
        try {
            BackupFileMeta meta = getMetaForBackupFile(delegatedFile.getUri());
            notifyBackupTaskStatusChanged(BackupTaskStatus.succeeded(taskToken, config, meta));
            notifyBackupAdded(meta);
        } catch (Exception e) {
            delegatedFile.delete();
            notifyBackupTaskStatusChanged(BackupTaskStatus.failed(taskToken, config, e));
        }
    }

    private class InternalDelegatedFile implements ApksBackupTaskExecutor.DelegatedFile {

        private BackupTaskConfig mConfig;
        private Uri mUri;

        private InternalDelegatedFile(BackupTaskConfig config) {
            mConfig = config;
        }

        @Override
        public OutputStream openOutputStream() throws Exception {
            mUri = createFileForTask(mConfig);
            return openFileOutputStream(mUri);
        }

        @Override
        public void delete() {
            if (mUri == null)
                return;

            deleteFile(mUri);
        }

        @Nullable
        private Uri getUri() {
            return mUri;
        }
    }
}
