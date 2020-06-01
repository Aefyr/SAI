package com.aefyr.sai.backup2.impl.storage;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.GuardedBy;
import androidx.annotation.Nullable;

import com.aefyr.sai.backup2.Backup;
import com.aefyr.sai.backup2.BackupComponent;
import com.aefyr.sai.backup2.backuptask.config.BackupTaskConfig;
import com.aefyr.sai.backup2.backuptask.config.BatchBackupTaskConfig;
import com.aefyr.sai.backup2.backuptask.config.SingleBackupTaskConfig;
import com.aefyr.sai.backup2.backuptask.executor.BatchBackupTaskExecutor;
import com.aefyr.sai.backup2.backuptask.executor.CancellableBackupTaskExecutor;
import com.aefyr.sai.backup2.backuptask.executor.SingleBackupTaskExecutor;
import com.aefyr.sai.backup2.impl.MutableBackup;
import com.aefyr.sai.backup2.impl.components.SimpleBackupComponent;
import com.aefyr.sai.backup2.impl.components.StandardComponentTypes;
import com.aefyr.sai.model.backup.SaiExportedAppMeta;
import com.aefyr.sai.model.backup.SaiExportedAppMeta2;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
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

    @GuardedBy("mTasks")
    private final Map<String, SingleBackupTaskConfig> mTasks = new HashMap<>();
    @GuardedBy("mTaskExecutors")
    private final Map<String, CancellableBackupTaskExecutor> mTaskExecutors = new HashMap<>();

    @GuardedBy("mBatchTasks")
    private final Map<String, BatchBackupTaskConfig> mBatchTasks = new HashMap<>();

    private HandlerThread mTaskProgressHandlerThread;
    private Handler mTaskProgressHandler;

    protected ApksBackupStorage() {
        mTaskProgressHandlerThread = new HandlerThread("ApksBackupStorage.TaskProgress");
        mTaskProgressHandlerThread.start();
        mTaskProgressHandler = new Handler(mTaskProgressHandlerThread.getLooper());
    }

    protected abstract Context getContext();

    protected abstract Uri createFileForTask(SingleBackupTaskConfig config) throws Exception;

    protected abstract OutputStream openFileOutputStream(Uri uri) throws Exception;

    protected abstract InputStream openFileInputStream(Uri uri) throws Exception;

    protected abstract void deleteFile(Uri uri);

    protected abstract long getFileSize(Uri uri);

    @Override
    public Backup getBackupByUri(Uri uri) throws Exception {

        MutableBackup mutableBackup = null;
        Uri iconUri = EMPTY_ICON;
        try (ZipInputStream zipInputStream = new ZipInputStream(openFileInputStream(uri))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (zipEntry.getName().equals(SaiExportedAppMeta.META_FILE)) {
                    if (mutableBackup != null)
                        continue;

                    SaiExportedAppMeta appMeta = SaiExportedAppMeta.deserialize(IOUtils.readStreamNoClose(zipInputStream));
                    mutableBackup = new MutableBackup();
                    mutableBackup.uri = uri;
                    mutableBackup.contentHash = getBackupFileHash(uri);

                    mutableBackup.pkg = appMeta.packageName();
                    mutableBackup.label = appMeta.label();
                    mutableBackup.versionCode = appMeta.versionCode();
                    mutableBackup.versionName = appMeta.versionName();
                    mutableBackup.exportTimestamp = appMeta.exportTime();
                    mutableBackup.storageId = getStorageId();
                    mutableBackup.components = Collections.singletonList(new SimpleBackupComponent(StandardComponentTypes.TYPE_APK_FILES, getFileSize(uri)));
                } else if (zipEntry.getName().equals(SaiExportedAppMeta2.META_FILE)) {
                    SaiExportedAppMeta2 appMeta = SaiExportedAppMeta2.deserialize(IOUtils.readStreamNoClose(zipInputStream));
                    mutableBackup = new MutableBackup();
                    mutableBackup.uri = uri;
                    mutableBackup.contentHash = getBackupFileHash(uri);

                    mutableBackup.pkg = appMeta.packageName();
                    mutableBackup.label = appMeta.label();
                    mutableBackup.versionCode = appMeta.versionCode();
                    mutableBackup.versionName = appMeta.versionName();
                    mutableBackup.isSplitApk = appMeta.isSplitApk();
                    mutableBackup.exportTimestamp = appMeta.exportTime();
                    mutableBackup.storageId = getStorageId();


                    List<BackupComponent> backupComponents = new ArrayList<>();
                    if (appMeta.backupComponents() != null) {
                        for (SaiExportedAppMeta2.BackupComponent backupComponent : appMeta.backupComponents()) {
                            backupComponents.add(new SimpleBackupComponent(backupComponent.type(), backupComponent.size()));
                        }
                    }
                    mutableBackup.components = backupComponents;
                } else if (zipEntry.getName().equals(SaiExportedAppMeta.ICON_FILE) || zipEntry.getName().equals(SaiExportedAppMeta2.ICON_FILE)) {
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

                if (mutableBackup != null && !EMPTY_ICON.equals(iconUri))
                    break;
            }
        }

        if (mutableBackup == null)
            throw new Exception("Meta file not found in archive");

        mutableBackup.iconUri = iconUri;

        return mutableBackup;
    }

    @Override
    public String createBackupTask(SingleBackupTaskConfig config) {
        String token = UUID.randomUUID().toString();

        synchronized (mTasks) {
            mTasks.put(token, config);
            notifyBackupTaskStatusChanged(BackupTaskStatus.created(token, config));
        }

        return token;
    }

    @Override
    public String createBatchBackupTask(BatchBackupTaskConfig config) {
        String token = UUID.randomUUID().toString();

        synchronized (mBatchTasks) {
            mBatchTasks.put(token, config);
            notifyBatchBackupTaskStatusChanged(BatchBackupTaskStatus.created(token));
        }

        return token;
    }

    @Override
    public void startBackupTask(String taskToken) {
        SingleBackupTaskConfig config;
        synchronized (mTasks) {
            config = mTasks.remove(taskToken);
        }
        if (config != null) {
            startSingleBackupTask(taskToken, config);
            return;
        }

        BatchBackupTaskConfig batchConfig;
        synchronized (mBatchTasks) {
            batchConfig = mBatchTasks.remove(taskToken);
        }
        if (batchConfig != null) {
            startBatchBackupTask(taskToken, batchConfig);
        }

    }

    private void startSingleBackupTask(String taskToken, SingleBackupTaskConfig config) {

        InternalDelegatedFile delegatedFile = new InternalDelegatedFile(config);
        ApksSingleBackupTaskExecutor taskExecutor = new ApksSingleBackupTaskExecutor(getContext(), config, delegatedFile);
        taskExecutor.setListener(new ApksSingleBackupTaskExecutor.Listener() {
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
            public void onSuccess(Backup backup) {
                notifyBackupTaskStatusChanged(BackupTaskStatus.succeeded(taskToken, config, backup));
                notifyBackupAdded(backup);
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

    private void startBatchBackupTask(String taskToken, BatchBackupTaskConfig batchConfig) {
        BatchBackupTaskExecutor taskExecutor = new BatchBackupTaskExecutor(getContext(), batchConfig, new InternalSingleBackupTaskExecutorFactory());
        taskExecutor.setListener(new BatchBackupTaskExecutor.Listener() {

            private Map<SingleBackupTaskConfig, Backup> mSucceededBackups = new HashMap<>();
            private Map<SingleBackupTaskConfig, Exception> mFailedBackups = new HashMap<>();

            @Override
            public void onStart() {

            }

            @Override
            public void onAppBackupStarted(SingleBackupTaskConfig config) {
                notifyBatchBackupTaskStatusChanged(BatchBackupTaskStatus.inProgress(taskToken, config, batchConfig.configs().size(), mSucceededBackups.size(), mFailedBackups.size()));
            }

            @Override
            public void onAppBackedUp(SingleBackupTaskConfig config, Backup backup) {
                mSucceededBackups.put(config, backup);
                notifyBackupAdded(backup);
            }

            @Override
            public void onAppBackupFailed(SingleBackupTaskConfig config, Exception e) {
                mFailedBackups.put(config, e);
            }

            @Override
            public void onCancelled(List<SingleBackupTaskConfig> cancelledBackups) {
                notifyBatchBackupTaskStatusChanged(BatchBackupTaskStatus.cancelled(taskToken, mSucceededBackups, mFailedBackups, cancelledBackups));
            }

            @Override
            public void onSuccess() {
                notifyBatchBackupTaskStatusChanged(BatchBackupTaskStatus.succeeded(taskToken, mSucceededBackups, mFailedBackups));
            }

            @Override
            public void onError(Exception e, List<SingleBackupTaskConfig> remainingBackups) {
                notifyBatchBackupTaskStatusChanged(BatchBackupTaskStatus.failed(taskToken, mSucceededBackups, mFailedBackups, remainingBackups, e));
            }
        }, mTaskProgressHandler);
        synchronized (mTaskExecutors) {
            mTaskExecutors.put(taskToken, taskExecutor);
        }
        notifyBatchBackupTaskStatusChanged(BatchBackupTaskStatus.queued(taskToken));
        taskExecutor.execute();
    }

    @Override
    public void cancelBackupTask(String taskToken) {
        synchronized (mTasks) {
            SingleBackupTaskConfig config = mTasks.remove(taskToken);
            if (config != null) {
                notifyBackupTaskStatusChanged(BackupTaskStatus.cancelled(taskToken, config));
                return;
            }
        }

        synchronized (mBatchTasks) {
            BatchBackupTaskConfig batchConfig = mBatchTasks.remove(taskToken);
            if (batchConfig != null) {
                notifyBatchBackupTaskStatusChanged(BatchBackupTaskStatus.cancelled(taskToken, Collections.emptyMap(), Collections.emptyMap(), batchConfig.configs()));
                return;
            }
        }

        synchronized (mTaskExecutors) {
            CancellableBackupTaskExecutor taskExecutor = mTaskExecutors.get(taskToken);
            if (taskExecutor != null)
                taskExecutor.requestCancellation();
        }
    }

    @Nullable
    @Override
    public BackupTaskConfig getTaskConfig(String taskToken) {
        //TODO use a single map for both classes or something
        synchronized (mTasks) {
            SingleBackupTaskConfig config = mTasks.get(taskToken);
            if (config != null)
                return config;
        }

        synchronized (mBatchTasks) {
            BatchBackupTaskConfig batchConfig = mBatchTasks.get(taskToken);
            if (batchConfig != null)
                return batchConfig;
        }

        return null;
    }

    private class InternalDelegatedFile implements ApksSingleBackupTaskExecutor.DelegatedFile {

        private SingleBackupTaskConfig mConfig;
        private Uri mUri;

        private InternalDelegatedFile(SingleBackupTaskConfig config) {
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

        @Override
        public Backup readMeta() throws Exception {
            return getBackupByUri(mUri);
        }
    }

    private class InternalSingleBackupTaskExecutorFactory implements BatchBackupTaskExecutor.SingleBackupTaskExecutorFactory {

        @Override
        public SingleBackupTaskExecutor createFor(SingleBackupTaskConfig config) {
            return new ApksSingleBackupTaskExecutor(getContext(), config, new InternalDelegatedFile(config));
        }
    }
}
