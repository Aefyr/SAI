package com.aefyr.sai.installer;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LongSparseArray;

import com.aefyr.sai.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SAIPackageInstaller {
    private static final String TAG = "SAIInstaller";

    public enum InstallationStatus {
        QUEUED, INSTALLING, INSTALLATION_SUCCEED, INSTALLATION_FAILED
    }

    @SuppressLint("StaticFieldLeak")//This is application context, lul
    private static SAIPackageInstaller sInstance;
    private Context mContext;

    private Handler mHandler = new Handler(Looper.getMainLooper());
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private ArrayDeque<QueuedInstallation> mInstallationQueue = new ArrayDeque<>();
    private ArrayList<InstallationStatusListener> mListeners = new ArrayList<>();
    private LongSparseArray<QueuedInstallation> mCreatedInstallationSessions = new LongSparseArray<>();

    private boolean mInstallationInProgress;
    private long lastInstallationID = 0;
    private long ongoingInstallationID;

    private BroadcastReceiver mFurtherInstallationEventsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getIntExtra(SAIService.EXTRA_INSTALLATION_STATUS, -1)) {
                case SAIService.STATUS_SUCCESS:
                    dispatchCurrentSessionUpdate(InstallationStatus.INSTALLATION_SUCCEED, intent.getStringExtra(SAIService.EXTRA_PACKAGE_NAME));
                    installationCompleted();
                    break;
                case SAIService.STATUS_FAILURE:
                    dispatchCurrentSessionUpdate(InstallationStatus.INSTALLATION_FAILED, intent.getStringExtra(SAIService.EXTRA_PACKAGE_NAME));
                    installationCompleted();
                    break;
            }
        }
    };

    public static SAIPackageInstaller getInstance(Context c) {
        return sInstance != null ? sInstance : new SAIPackageInstaller(c);
    }

    private SAIPackageInstaller(Context c) {
        Log.d(TAG, "New instance created");
        mContext = c.getApplicationContext();
        mContext.registerReceiver(mFurtherInstallationEventsReceiver, new IntentFilter(SAIService.ACTION_INSTALLATION_STATUS_NOTIFICATION));
        sInstance = this;
    }

    public interface InstallationStatusListener {
        void onStatusChanged(long installationID, InstallationStatus status, String packageName);
    }

    public void addStatusListener(InstallationStatusListener listener) {
        mListeners.add(listener);
    }

    public void removeStatusListener(InstallationStatusListener listener) {
        mListeners.remove(listener);
    }

    public long createInstallationSession(List<File> apkFiles) {
        long installationID = lastInstallationID++;
        mCreatedInstallationSessions.put(installationID, new QueuedInstallation(apkFiles, installationID));
        return installationID;
    }

    public void startInstallationSession(long sessionID) {
        QueuedInstallation installation = mCreatedInstallationSessions.get(sessionID);
        mCreatedInstallationSessions.remove(sessionID);
        if (installation == null)
            return;

        mInstallationQueue.addLast(installation);
        dispatchSessionUpdate(installation.id, InstallationStatus.QUEUED, null);
        processQueue();
    }

    public boolean isInstallationInProgress() {
        return mInstallationInProgress;
    }

    private void processQueue() {
        if (mInstallationQueue.size() == 0 || mInstallationInProgress)
            return;

        QueuedInstallation installation = mInstallationQueue.removeFirst();
        List<File> apkFiles = installation.apkFiles;
        ongoingInstallationID = installation.id;
        mInstallationInProgress = true;

        dispatchCurrentSessionUpdate(InstallationStatus.INSTALLING, null);

        mExecutor.execute(() -> {
            PackageInstaller packageInstaller = mContext.getPackageManager().getPackageInstaller();
            try {
                Thread.sleep(5000);
                PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                int sessionID = packageInstaller.createSession(sessionParams);

                PackageInstaller.Session session = packageInstaller.openSession(sessionID);
                for (File apkFile : apkFiles) {
                    InputStream inputStream = new FileInputStream(apkFile);
                    OutputStream outputStream = session.openWrite(apkFile.getName(), 0, -1);
                    IOUtils.copyStream(inputStream, outputStream);
                    session.fsync(outputStream);
                    inputStream.close();
                    outputStream.close();
                }

                Intent callbackIntent = new Intent(mContext, SAIService.class);
                PendingIntent pendingIntent = PendingIntent.getService(mContext, 0, callbackIntent, 0);
                session.commit(pendingIntent.getIntentSender());
                session.close();
            } catch (Exception e) {
                Log.w(TAG, e);
                dispatchCurrentSessionUpdate(InstallationStatus.INSTALLATION_FAILED, null);
            }
        });
    }

    private void installationCompleted() {
        mInstallationInProgress = false;
        ongoingInstallationID = -1;
        processQueue();
    }

    private void dispatchSessionUpdate(long sessionID, InstallationStatus status, String packageName) {
        mHandler.post(() -> {
            for (InstallationStatusListener listener : mListeners)
                listener.onStatusChanged(sessionID, status, packageName);
        });
    }

    private void dispatchCurrentSessionUpdate(InstallationStatus status, String packageName) {
        dispatchSessionUpdate(ongoingInstallationID, status, packageName);
    }
}
