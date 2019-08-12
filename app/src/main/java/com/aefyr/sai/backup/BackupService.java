package com.aefyr.sai.backup;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.aefyr.sai.R;
import com.aefyr.sai.model.backup.PackageMeta;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class BackupService extends Service {
    private static final String TAG = "BackupService";

    public static String EXTRA_PACKAGE_META = "package";
    public static String EXTRA_DESTINATION = "destination";

    private static final int NOTIFICATION_ID = 322;
    private static final String NOTIFICATION_CHANNEL_ID = "backup_service";
    private static final int PROGRESS_NOTIFICATION_UPDATE_CD = 500;


    private NotificationManagerCompat mNotificationManager;
    private Random mRandom = new Random();

    private Pair<PackageMeta, Uri> mCurrentBackup;
    private long mLastProgressUpdate = 0;
    private Deque<Pair<PackageMeta, Uri>> mQueue = new ConcurrentLinkedDeque<>();

    private Executor mExecutor = Executors.newSingleThreadExecutor();
    private Handler mHandler = new Handler(Looper.getMainLooper());

    public static void enqueueBackup(Context c, PackageMeta packageMeta, Uri destination) {
        Intent intent = new Intent(c, BackupService.class);
        intent.putExtra(EXTRA_PACKAGE_META, packageMeta);
        intent.putExtra(EXTRA_DESTINATION, destination);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            c.startForegroundService(intent);
        } else {
            c.startService(intent);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        prepareNotificationsStuff();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        PackageMeta packageMeta = intent.getParcelableExtra(EXTRA_PACKAGE_META);
        Uri destination = intent.getParcelableExtra(EXTRA_DESTINATION);
        enqueue(packageMeta, destination);
        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void enqueue(PackageMeta packageMeta, Uri destination) {
        mQueue.add(new Pair<>(packageMeta, destination));
        processQueue();
    }

    private void processQueue() {
        if (mCurrentBackup != null)
            return;

        if (mQueue.isEmpty()) {
            die();
            return;
        }

        mCurrentBackup = mQueue.remove();
        publishProgress(0, 1, true);
        mExecutor.execute(() -> backup(mCurrentBackup.first, mCurrentBackup.second));
    }

    private void backupCompleted(boolean successfully) {
        notifyBackupCompleted(mCurrentBackup.first, successfully);
        mCurrentBackup = null;
        processQueue();
    }

    private void backup(PackageMeta packageMeta, Uri destination) {
        try (ZipOutputStream zipOutputStream = new ZipOutputStream(getContentResolver().openOutputStream(destination));) {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(packageMeta.packageName, 0);

            List<File> apkFiles = new ArrayList<>();
            apkFiles.add(new File(applicationInfo.publicSourceDir));

            if (applicationInfo.splitPublicSourceDirs != null) {
                for (String splitPath : applicationInfo.splitPublicSourceDirs)
                    apkFiles.add(new File(splitPath));
            }

            long currentProgress = 0;
            long maxProgress = 0;
            for (File apkFile : apkFiles) {
                maxProgress += apkFile.length();
            }

            for (File apkFile : apkFiles) {
                zipOutputStream.setMethod(ZipOutputStream.STORED);

                ZipEntry zipEntry = new ZipEntry(apkFile.getName());
                zipEntry.setMethod(ZipEntry.STORED);
                zipEntry.setCompressedSize(apkFile.length());
                zipEntry.setSize(apkFile.length());
                zipEntry.setCrc(IOUtils.calculateFileCrc32(apkFile));

                zipOutputStream.putNextEntry(zipEntry);

                try (FileInputStream apkInputStream = new FileInputStream(apkFile)) {
                    byte[] buffer = new byte[1024 * 256];
                    int read;

                    while ((read = apkInputStream.read(buffer)) > 0) {
                        zipOutputStream.write(buffer, 0, read);
                        currentProgress += read;
                        publishProgress(currentProgress, maxProgress, false);
                    }
                    IOUtils.copyStream(apkInputStream, zipOutputStream);
                }

                zipOutputStream.closeEntry();
            }

            mHandler.post(() -> backupCompleted(true));
        } catch (Exception e) {
            Log.w(TAG, e);
            mHandler.post(() -> backupCompleted(false));
        }


    }

    private void die() {
        stopForeground(true);
        stopSelf();
    }

    private void publishProgress(long current, long goal, boolean force) {
        int progress = (int) (current / (goal / 100));
        publishProgress(progress, 100, force);
    }

    private void publishProgress(int current, int goal, boolean force) {
        if (System.currentTimeMillis() - mLastProgressUpdate < PROGRESS_NOTIFICATION_UPDATE_CD && !force)
            return;

        mLastProgressUpdate = System.currentTimeMillis();

        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_backup)
                .setContentTitle(getString(R.string.backup_backup))
                .setProgress(goal, current, false)
                .setContentText(getString(R.string.backup_backup_in_progress, mCurrentBackup.first.label))
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void notifyBackupCompleted(PackageMeta packageMeta, boolean successfully) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_backup)
                .setContentTitle(getString(R.string.backup_backup));

        if (successfully) {
            builder.setContentText(getString(R.string.backup_backup_success, packageMeta.label));
        } else {
            builder.setContentText(getString(R.string.backup_backup_failed, packageMeta.label));
        }

        mNotificationManager.notify(1000 + mRandom.nextInt(100000), builder.build());
    }

    private void prepareNotificationsStuff() {
        mNotificationManager = NotificationManagerCompat.from(this);

        if (Utils.apiIsAtLeast(Build.VERSION_CODES.O)) {
            mNotificationManager.createNotificationChannel(new NotificationChannel(NOTIFICATION_CHANNEL_ID, getString(R.string.backup_backup), NotificationManager.IMPORTANCE_DEFAULT));
        }

        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_backup)
                .setContentTitle(getString(R.string.backup_backup))
                .setContentText("idle")
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

}
