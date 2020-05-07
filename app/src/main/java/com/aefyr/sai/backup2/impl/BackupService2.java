package com.aefyr.sai.backup2.impl;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.aefyr.sai.R;
import com.aefyr.sai.backup2.BackupFileMeta;
import com.aefyr.sai.backup2.BackupStorage;
import com.aefyr.sai.backup2.BackupTaskConfig;
import com.aefyr.sai.model.common.PackageMeta;
import com.aefyr.sai.utils.NotificationHelper;
import com.aefyr.sai.utils.Utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class BackupService2 extends Service implements BackupStorage.BackupProgressListener {
    private static final String TAG = "BackupService";
    private static final int NOTIFICATION_ID = 322;
    private static final String NOTIFICATION_CHANNEL_ID = "backup_service";
    private static final int PROGRESS_NOTIFICATION_UPDATE_CD = 500;

    public static String EXTRA_TASK_CONFIG = "config";

    private NotificationHelper mNotificationHelper;
    private Random mRandom = new Random();

    private Map<String, BackupTaskInfo> mTasks = new HashMap<>();

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private HandlerThread mProgressHandlerThread;
    private Handler mProgressHandler;

    private BackupStorage mStorage;

    public static void enqueueBackup(Context c, BackupTaskConfig config) {
        Intent intent = new Intent(c, BackupService2.class);
        intent.putExtra(EXTRA_TASK_CONFIG, config);
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
        BackupTaskConfig config = intent.getParcelableExtra(EXTRA_TASK_CONFIG);
        enqueue(config);

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
    private void enqueue(BackupTaskConfig backupTaskConfig) {
        String tag = generateTag();
        //TODO id probably shouldn't be just random
        mTasks.put(tag, new BackupTaskInfo(backupTaskConfig.packageMeta(), 1000 + mRandom.nextInt(100000)));
        updateStatus();

        mStorage.backupApp(backupTaskConfig, tag);
    }

    @MainThread
    private void taskFinished(String taskTag) {
        mTasks.remove(taskTag);
        updateStatus();
    }

    @MainThread
    private void updateStatus() {
        if (mTasks.isEmpty()) {
            die();
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

        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_backup)
                .setContentTitle(getString(R.string.backup_backup))
                .setContentText(getText(R.string.backup_backup_export_in_progress))
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    @Override
    public void onProgressChanged(String tag, long current, long goal) {
        int progress = (int) (current / (goal / 100));
        publishProgress(mTasks.get(tag), progress, 100);
    }

    @Override
    public void onBackupCompleted(String tag, BackupFileMeta backupFileMeta) {
        notifyBackupCompleted(mTasks.get(tag), true);
        mHandler.post(() -> taskFinished(tag));
    }

    @Override
    public void onBackupFailed(String tag, Exception e) {
        notifyBackupCompleted(mTasks.get(tag), false);
        mHandler.post(() -> taskFinished(tag));
    }

    private void publishProgress(BackupTaskInfo taskInfo, int current, int goal) {
        if (System.currentTimeMillis() - taskInfo.lastProgressUpdate < PROGRESS_NOTIFICATION_UPDATE_CD)
            return;

        taskInfo.lastProgressUpdate = System.currentTimeMillis();

        Notification notification = new NotificationCompat.Builder(BackupService2.this, NOTIFICATION_CHANNEL_ID)
                .setOnlyAlertOnce(true)
                .setWhen(taskInfo.creationTime)
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_backup)
                .setContentTitle(getString(R.string.backup_backup))
                .setProgress(goal, current, false)
                .setContentText(getString(R.string.backup_backup_in_progress, taskInfo.packageMeta.label))
                .build();

        mNotificationHelper.notify(taskInfo.notificationId, notification, true);
    }

    private void notifyBackupCompleted(BackupTaskInfo taskInfo, boolean successfully) {
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

        mNotificationHelper.notify(taskInfo.notificationId, builder.build(), false);
    }

    private static class BackupTaskInfo {
        PackageMeta packageMeta;
        int notificationId;
        long lastProgressUpdate = 0;
        long creationTime = System.currentTimeMillis();

        private BackupTaskInfo(PackageMeta packageMeta, int notificationId) {
            this.packageMeta = packageMeta;
            this.notificationId = notificationId;
        }
    }

    private String generateTag() {
        return UUID.randomUUID().toString();
    }
}
