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
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.aefyr.sai.R;
import com.aefyr.sai.model.common.PackageMeta;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.NotificationHelper;
import com.aefyr.sai.utils.Utils;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

//TODO add more consistency in case of backup fail
public class BackupService extends Service {
    private static final String TAG = "BackupService";

    public static String EXTRA_TASK_CONFIG = "config";

    private static final int NOTIFICATION_ID = 322;
    private static final String NOTIFICATION_CHANNEL_ID = "backup_service";
    private static final int PROGRESS_NOTIFICATION_UPDATE_CD = 500;


    private NotificationHelper mNotificationHelper;
    private Random mRandom = new Random();

    private Set<BackupTask> mTasks = new HashSet<>();

    private Executor mExecutor = Executors.newFixedThreadPool(4);
    private Handler mHandler = new Handler(Looper.getMainLooper());

    public static void enqueueBackup(Context c, BackupTaskConfig config) {
        Intent intent = new Intent(c, BackupService.class);
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

    @MainThread
    private void enqueue(BackupTaskConfig backupTaskConfig) {
        BackupTask backupTask = new BackupTask(backupTaskConfig);
        mTasks.add(backupTask);

        updateStatus();
        mExecutor.execute(backupTask::execute);
    }

    @MainThread
    private void taskFinished(BackupTask backupTask) {
        mTasks.remove(backupTask);
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

    private class BackupTask {
        BackupTaskConfig config;

        private long mLastProgressUpdate;
        private int mProgressNotificationId;
        private long mTaskCreationTime = System.currentTimeMillis();

        BackupTask(BackupTaskConfig config) {
            this.config = config;

            //TODO id probably shouldn't be just random
            mProgressNotificationId = 1000 + mRandom.nextInt(100000);
        }

        private void publishProgress(long current, long goal) {
            int progress = (int) (current / (goal / 100));
            publishProgress(progress, 100);
        }

        private void publishProgress(int current, int goal) {
            if (System.currentTimeMillis() - mLastProgressUpdate < PROGRESS_NOTIFICATION_UPDATE_CD)
                return;

            mLastProgressUpdate = System.currentTimeMillis();

            Notification notification = new NotificationCompat.Builder(BackupService.this, NOTIFICATION_CHANNEL_ID)
                    .setOnlyAlertOnce(true)
                    .setWhen(mTaskCreationTime)
                    .setOngoing(true)
                    .setSmallIcon(R.drawable.ic_backup)
                    .setContentTitle(getString(R.string.backup_backup))
                    .setProgress(goal, current, false)
                    .setContentText(getString(R.string.backup_backup_in_progress, config.packageMeta.label))
                    .build();

            mNotificationHelper.notify(mProgressNotificationId, notification, true);
        }

        private void finished() {
            notifyBackupCompleted(true);
            mHandler.post(() -> BackupService.this.taskFinished(this));
        }

        private void failed(@Nullable Exception e) {
            notifyBackupCompleted(false);
            mHandler.post(() -> BackupService.this.taskFinished(this));
        }

        private void notifyBackupCompleted(boolean successfully) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(BackupService.this, NOTIFICATION_CHANNEL_ID)
                    .setWhen(System.currentTimeMillis())
                    .setOnlyAlertOnce(false)
                    .setOngoing(false)
                    .setSmallIcon(R.drawable.ic_backup)
                    .setContentTitle(getString(R.string.backup_backup));

            if (successfully) {
                builder.setContentText(getString(R.string.backup_backup_success, config.packageMeta.label));
            } else {
                builder.setContentText(getString(R.string.backup_backup_failed, config.packageMeta.label));
            }

            mNotificationHelper.notify(mProgressNotificationId, builder.build(), false);
        }

        private List<File> getAllApkFilesForPackage(String pkg) throws Exception {
            ApplicationInfo applicationInfo = getPackageManager().getApplicationInfo(pkg, 0);

            List<File> apkFiles = new ArrayList<>();
            apkFiles.add(new File(applicationInfo.publicSourceDir));

            if (applicationInfo.splitPublicSourceDirs != null) {
                for (String splitPath : applicationInfo.splitPublicSourceDirs)
                    apkFiles.add(new File(splitPath));
            }

            return apkFiles;
        }

        void execute() {
            Uri destination = config.destination;

            try (ZipOutputStream zipOutputStream = new ZipOutputStream(getContentResolver().openOutputStream(destination))) {
                List<File> apkFiles;
                if (config.apksToBackup.size() == 0)
                    apkFiles = getAllApkFilesForPackage(config.packageMeta.packageName);
                else
                    apkFiles = config.apksToBackup;

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
                        byte[] buffer = new byte[1024 * 512];
                        int read;

                        while ((read = apkInputStream.read(buffer)) > 0) {
                            zipOutputStream.write(buffer, 0, read);
                            currentProgress += read;
                            publishProgress(currentProgress, maxProgress);
                        }
                        IOUtils.copyStream(apkInputStream, zipOutputStream);
                    }
                    zipOutputStream.closeEntry();
                }
                finished();
            } catch (Exception e) {
                Log.w(TAG, e);
                failed(e);
            }
        }
    }

    public static class BackupTaskConfig implements Parcelable {
        private PackageMeta packageMeta;
        private ArrayList<File> apksToBackup = new ArrayList<>();
        private Uri destination;

        private BackupTaskConfig(PackageMeta packageMeta, Uri destination) {
            this.packageMeta = packageMeta;
            this.destination = destination;
        }

        BackupTaskConfig(Parcel in) {
            packageMeta = in.readParcelable(PackageMeta.class.getClassLoader());

            ArrayList<String> apkFilePaths = new ArrayList<>();
            in.readStringList(apkFilePaths);
            for (String apkFilePath : apkFilePaths)
                apksToBackup.add(new File(apkFilePath));

            destination = in.readParcelable(Uri.class.getClassLoader());
        }

        public static final Creator<BackupTaskConfig> CREATOR = new Creator<BackupTaskConfig>() {
            @Override
            public BackupTaskConfig createFromParcel(Parcel in) {
                return new BackupTaskConfig(in);
            }

            @Override
            public BackupTaskConfig[] newArray(int size) {
                return new BackupTaskConfig[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeParcelable(packageMeta, flags);

            ArrayList<String> apkFilePaths = new ArrayList<>();
            for (File apkFile : apksToBackup)
                apkFilePaths.add(apkFile.getAbsolutePath());
            dest.writeStringList(apkFilePaths);

            dest.writeParcelable(destination, 0);
        }

        public static class Builder {
            private BackupTaskConfig mConfig;

            public Builder(PackageMeta packageMeta, Uri destination) {
                mConfig = new BackupTaskConfig(packageMeta, destination);
            }

            public Builder addApk(File apkFile) {
                mConfig.apksToBackup.add(apkFile);
                return this;
            }

            public Builder addAllApks(Collection<File> apkFiles) {
                mConfig.apksToBackup.addAll(apkFiles);
                return this;
            }

            public BackupTaskConfig build() {
                return mConfig;
            }
        }


    }
}
