package com.aefyr.sai.backup2.impl;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.aefyr.sai.BuildConfig;
import com.aefyr.sai.R;
import com.aefyr.sai.backup2.BackupStorage;
import com.aefyr.sai.backup2.backuptask.config.BackupTaskConfig;
import com.aefyr.sai.backup2.backuptask.config.BatchBackupTaskConfig;
import com.aefyr.sai.backup2.backuptask.config.SingleBackupTaskConfig;
import com.aefyr.sai.backup2.impl.storage.LocalBackupStorage;
import com.aefyr.sai.model.common.PackageMeta;
import com.aefyr.sai.utils.NotificationHelper;
import com.aefyr.sai.utils.Utils;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

//TODO make this prettier
public class BackupService2 extends Service implements BackupStorage.BackupProgressListener {
    private static final String TAG = "BackupService";
    private static final int NOTIFICATION_ID = 322;
    private static final String NOTIFICATION_CHANNEL_ID = "backup_service";
    private static final int PROGRESS_NOTIFICATION_UPDATE_CD = 500;

    public static final String ACTION_ENQUEUE_BACKUP = BuildConfig.APPLICATION_ID + ".action.BackupService2.ENQUEUE_BACKUP";

    public static final String ACTION_CANCEL_BACKUP = BuildConfig.APPLICATION_ID + ".action.BackupService2.CANCEL_BACKUP";
    public static final String EXTRA_TASK_TOKEN = "task_token";

    public static final String NOTIFICATION_GROUP_BACKUP_ONGOING = BuildConfig.APPLICATION_ID + ".notification_group.BACKUP_ONGOING";
    public static final String NOTIFICATION_GROUP_BACKUP_DONE = BuildConfig.APPLICATION_ID + ".notification_group.BACKUP_DONE";

    private NotificationHelper mNotificationHelper;

    private Map<String, BackupTaskInfo> mTasks = new ConcurrentHashMap<>();
    private Map<String, BatchBackupTaskInfo> mBatchTasks = new ConcurrentHashMap<>();

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private HandlerThread mProgressHandlerThread;
    private Handler mProgressHandler;

    private BackupStorage mStorage;

    public static void enqueueBackup(Context c, String taskToken) {
        Intent intent = new Intent(c, BackupService2.class);
        intent.setAction(ACTION_ENQUEUE_BACKUP);
        intent.putExtra(EXTRA_TASK_TOKEN, taskToken);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            c.startForegroundService(intent);
        } else {
            c.startService(intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mProgressHandlerThread = new HandlerThread("BackupService2.Progress");
        mProgressHandlerThread.start();
        mProgressHandler = new Handler(mProgressHandlerThread.getLooper());

        mStorage = LocalBackupStorage.getInstance(getApplicationContext());
        mStorage.addBackupProgressListener(this, mProgressHandler);
        prepareNotificationsStuff();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        switch (Objects.requireNonNull(intent.getAction())) {
            case ACTION_ENQUEUE_BACKUP: {
                String taskToken = intent.getStringExtra(EXTRA_TASK_TOKEN);
                enqueue(taskToken);
                break;
            }
            case ACTION_CANCEL_BACKUP:
                String taskToken = intent.getStringExtra(EXTRA_TASK_TOKEN);
                cancelBackup(taskToken);
                break;
        }


        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mStorage.removeBackupProgressListener(this);
        mProgressHandlerThread.quitSafely();
    }

    @MainThread
    private void cancelBackup(String taskToken) {
        mStorage.cancelBackupTask(taskToken);
    }

    @MainThread
    private void enqueue(String taskToken) {

        BackupTaskConfig config = mStorage.getTaskConfig(taskToken);
        if (config == null)
            return;

        if (config instanceof SingleBackupTaskConfig) {
            SingleBackupTaskConfig taskConfig = (SingleBackupTaskConfig) config;
            mTasks.put(taskToken, new BackupTaskInfo(taskConfig.packageMeta(), taskToken, taskToken));
        } else if (config instanceof BatchBackupTaskConfig) {
            mBatchTasks.put(taskToken, new BatchBackupTaskInfo(taskToken, taskToken));
        } else {
            Log.w(TAG, String.format("Got unsupported task config class - %s, task token - %s, ignoring", config.getClass().getCanonicalName(), taskToken));
        }

        updateStatus();
        mStorage.startBackupTask(taskToken);
    }

    @MainThread
    private void taskFinished(String taskTag) {
        mTasks.remove(taskTag);
        mBatchTasks.remove(taskTag);
        updateStatus();
    }

    @MainThread
    private void updateStatus() {
        if (mTasks.isEmpty() && mBatchTasks.isEmpty()) {
            die();
        } else {
            startForeground(NOTIFICATION_ID, buildStatusNotification());
        }
    }

    private void die() {
        stopForeground(true);
        stopSelf();
    }

    private void prepareNotificationsStuff() {
        NotificationManagerCompat mNotificationManager = NotificationManagerCompat.from(this);
        mNotificationHelper = NotificationHelper.getInstance(this);

        if (Utils.apiIsAtLeast(Build.VERSION_CODES.O)) {
            mNotificationManager.createNotificationChannel(new NotificationChannel(NOTIFICATION_CHANNEL_ID, getString(R.string.backup_backup), NotificationManager.IMPORTANCE_DEFAULT));
        }

        startForeground(NOTIFICATION_ID, buildStatusNotification());
    }

    private Notification buildStatusNotification() {
        return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_backup)
                .setContentTitle(getString(R.string.backup_backup))
                .setContentText(getString(R.string.backup_backup_export_in_progress_2, mTasks.size() + mBatchTasks.size()))
                .build();
    }

    private void publishProgress(BackupTaskInfo taskInfo, int current, int goal) {
        if (System.currentTimeMillis() - taskInfo.lastProgressUpdate < PROGRESS_NOTIFICATION_UPDATE_CD)
            return;

        taskInfo.lastProgressUpdate = System.currentTimeMillis();

        PendingIntent cancelTaskPendingIntent = taskInfo.cachedCancelPendingIntent;
        if (cancelTaskPendingIntent == null) {
            Intent cancelTaskIntent = new Intent(this, BackupService2.class);
            cancelTaskIntent.setData(new Uri.Builder().scheme("cancel").path(taskInfo.taskToken).build());
            cancelTaskIntent.setAction(ACTION_CANCEL_BACKUP);
            cancelTaskIntent.putExtra(EXTRA_TASK_TOKEN, taskInfo.taskToken);

            cancelTaskPendingIntent = PendingIntent.getService(this, 0, cancelTaskIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            taskInfo.cachedCancelPendingIntent = cancelTaskPendingIntent;
        }

        Notification notification = new NotificationCompat.Builder(BackupService2.this, NOTIFICATION_CHANNEL_ID)
                .setOnlyAlertOnce(true)
                .setWhen(taskInfo.creationTime)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_backup)
                .setContentTitle(getString(R.string.backup_backup))
                .setProgress(goal, current, false)
                .setContentText(getString(R.string.backup_backup_in_progress, taskInfo.packageMeta.label))
                .addAction(new NotificationCompat.Action(null, getString(R.string.cancel), cancelTaskPendingIntent))
                .build();

        mNotificationHelper.notify(taskInfo.notificationTag, 0, notification, taskInfo.firstProgressNotificationFired);
        taskInfo.firstProgressNotificationFired = true;
    }

    private void notifyBackupCancelled(BackupTaskInfo taskInfo) {
        if (taskInfo.cachedCancelPendingIntent != null)
            taskInfo.cachedCancelPendingIntent.cancel();

        mNotificationHelper.cancel(taskInfo.notificationTag, 0);
    }

    private void notifyBackupCompleted(BackupTaskInfo taskInfo, boolean successfully) {
        if (taskInfo.cachedCancelPendingIntent != null)
            taskInfo.cachedCancelPendingIntent.cancel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(BackupService2.this, NOTIFICATION_CHANNEL_ID)
                .setWhen(System.currentTimeMillis())
                .setOnlyAlertOnce(false)
                .setOngoing(false)
                .setSmallIcon(R.drawable.ic_backup)
                .setContentTitle(getString(R.string.backup_backup));

        if (successfully) {
            builder.setContentText(getString(R.string.backup_backup_success, taskInfo.packageMeta.label));
        } else {
            builder.setContentText(getString(R.string.backup_backup_failed, taskInfo.packageMeta.label));
        }

        mNotificationHelper.notify(taskInfo.notificationTag, 0, builder.build(), false);
    }

    @Override
    public void onBackupTaskStatusChanged(String storageId, BackupStorage.BackupTaskStatus status) {
        switch (status.state()) {
            case CREATED:
            case QUEUED:
                break;
            case IN_PROGRESS:
                int progress = (int) ((float) status.currentProgress() / ((float) status.progressGoal() / 100f));
                publishProgress(mTasks.get(status.token()), progress, 100);
                break;
            case CANCELLED:
                notifyBackupCancelled(mTasks.get(status.token()));
                mHandler.post(() -> taskFinished(status.token()));
                break;
            case SUCCEEDED:
                notifyBackupCompleted(mTasks.get(status.token()), true);
                mHandler.post(() -> taskFinished(status.token()));
                break;
            case FAILED:
                notifyBackupCompleted(mTasks.get(status.token()), false);
                mHandler.post(() -> taskFinished(status.token()));
                break;
        }
    }

    private void publishBatchProgress(BatchBackupTaskInfo taskInfo, int current, int goal, SingleBackupTaskConfig currentBackupConfig) {
        if (System.currentTimeMillis() - taskInfo.lastProgressUpdate < PROGRESS_NOTIFICATION_UPDATE_CD)
            return;

        taskInfo.lastProgressUpdate = System.currentTimeMillis();

        PendingIntent cancelTaskPendingIntent = taskInfo.cachedCancelPendingIntent;
        if (cancelTaskPendingIntent == null) {
            Intent cancelTaskIntent = new Intent(this, BackupService2.class);
            cancelTaskIntent.setData(new Uri.Builder().scheme("cancel").path(taskInfo.taskToken).build());
            cancelTaskIntent.setAction(ACTION_CANCEL_BACKUP);
            cancelTaskIntent.putExtra(EXTRA_TASK_TOKEN, taskInfo.taskToken);

            cancelTaskPendingIntent = PendingIntent.getService(this, 0, cancelTaskIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            taskInfo.cachedCancelPendingIntent = cancelTaskPendingIntent;
        }

        Notification notification = new NotificationCompat.Builder(BackupService2.this, NOTIFICATION_CHANNEL_ID)
                .setOnlyAlertOnce(true)
                .setWhen(taskInfo.creationTime)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_backup)
                .setContentTitle(getString(R.string.backup_batch_backup))
                .setProgress(goal, current, false)
                .setContentText(getString(R.string.backup_backup_in_progress, currentBackupConfig.packageMeta().label))
                .addAction(new NotificationCompat.Action(null, getString(R.string.cancel), cancelTaskPendingIntent))
                .build();

        mNotificationHelper.notify(taskInfo.notificationTag, 0, notification, taskInfo.firstProgressNotificationFired);
        taskInfo.firstProgressNotificationFired = true;
    }

    private void notifyBatchBackupCompleted(BatchBackupTaskInfo taskInfo, BackupStorage.BatchBackupTaskStatus status) {
        if (taskInfo.cachedCancelPendingIntent != null)
            taskInfo.cachedCancelPendingIntent.cancel();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(BackupService2.this, NOTIFICATION_CHANNEL_ID)
                .setWhen(System.currentTimeMillis())
                .setOnlyAlertOnce(false)
                .setOngoing(false)
                .setSmallIcon(R.drawable.ic_backup)
                .setContentTitle(getString(R.string.backup_batch_backup_completed))
                .setStyle(new NotificationCompat.BigTextStyle());

        StringBuilder resultSb = new StringBuilder();
        if (!status.succeededBackups().isEmpty()) {
            appendWithNewLine(resultSb, getString(R.string.backup_batch_backup_result_succeeded, status.succeededBackups().size()));
        }

        if (!status.failedBackups().isEmpty()) {
            appendWithNewLine(resultSb, getString(R.string.backup_batch_backup_result_failed, status.failedBackups().size()));
        }

        if ((status.state() == BackupStorage.BackupTaskState.CANCELLED || status.state() == BackupStorage.BackupTaskState.FAILED) && !status.cancelledBackups().isEmpty()) {
            appendWithNewLine(resultSb, getString(R.string.backup_batch_backup_result_cancelled, status.cancelledBackups().size()));
        }

        builder.setContentText(resultSb.toString());

        mNotificationHelper.notify(taskInfo.notificationTag, 0, builder.build(), false);
    }

    private void appendWithNewLine(StringBuilder sb, CharSequence text) {
        if (sb.length() > 0)
            sb.append("\n");

        sb.append(text);
    }

    @Override
    public void onBatchBackupTaskStatusChanged(String storageId, BackupStorage.BatchBackupTaskStatus status) {
        switch (status.state()) {
            case CREATED:
            case QUEUED:
                break;
            case IN_PROGRESS:
                publishBatchProgress(mBatchTasks.get(status.token()), status.completedBackupsCount(), status.totalBackupsCount(), status.currentConfig());
                break;
            case CANCELLED:
            case SUCCEEDED:
            case FAILED:
                notifyBatchBackupCompleted(mBatchTasks.get(status.token()), status);
                mHandler.post(() -> taskFinished(status.token()));
                break;
        }
    }

    private static class BackupTaskInfo {
        PackageMeta packageMeta;
        String taskToken;
        String notificationTag;
        long lastProgressUpdate = 0;
        long creationTime = System.currentTimeMillis();
        PendingIntent cachedCancelPendingIntent;
        boolean firstProgressNotificationFired = false;

        private BackupTaskInfo(PackageMeta packageMeta, String taskToken, String notificationTag) {
            this.packageMeta = packageMeta;
            this.taskToken = taskToken;
            this.notificationTag = notificationTag;
        }
    }

    private static class BatchBackupTaskInfo {
        String taskToken;
        String notificationTag;
        long lastProgressUpdate = 0;
        long creationTime = System.currentTimeMillis();
        PendingIntent cachedCancelPendingIntent;
        boolean firstProgressNotificationFired = false;

        private BatchBackupTaskInfo(String taskToken, String notificationTag) {
            this.taskToken = taskToken;
            this.notificationTag = notificationTag;
        }
    }
}
