package com.aefyr.sai.installer;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.aefyr.sai.utils.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

public class SAIPackageInstaller {
    private static final String TAG = "SAIInstaller";

    public enum InstallationStatus {
        INSTALLING, AWAITING_USER_CONFIRMATION, INSTALLATION_SUCCEED, INSTALLATION_FAILED, UNKNOWN
    }

    private static SAIPackageInstaller sInstance;
    private Context mContext;
    private HashMap<Integer, InstallationStatusListener> mListeners;
    private Handler mHandler;

    private BroadcastReceiver mFurtherInstallationEventsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            InstallationStatus installationStatus = InstallationStatus.UNKNOWN;
            switch (intent.getIntExtra(SAIService.EXTRA_INSTALLATION_STATUS, -1)) {
                case SAIService.STATUS_SUCCESS:
                    installationStatus = InstallationStatus.INSTALLATION_SUCCEED;
                    break;
                case SAIService.STATUS_CONFIRMATION_PENDING:
                    installationStatus = InstallationStatus.AWAITING_USER_CONFIRMATION;
                    break;
                case SAIService.STATUS_FAILURE:
                    installationStatus = InstallationStatus.INSTALLATION_FAILED;
                    break;
            }

            dispatchSessionListenerStatusUpdate(intent.getIntExtra(SAIService.EXTRA_SESSION_ID, -1), installationStatus);
        }
    };

    public static SAIPackageInstaller getInstance(Context c) {
        return sInstance != null ? sInstance : new SAIPackageInstaller(c);
    }

    private SAIPackageInstaller(Context c) {
        mContext = c.getApplicationContext();
        mListeners = new HashMap<>();
        mHandler = new Handler(Looper.getMainLooper());
        mContext.registerReceiver(mFurtherInstallationEventsReceiver, new IntentFilter(SAIService.ACTION_INSTALLATION_STATUS_NOTIFICATION));
        sInstance = this;
    }

    public interface InstallationStatusListener {
        void onStatusChanged(InstallationStatus status);
    }

    public void installApks(List<File> apkFiles, InstallationStatusListener listener) {
        listener.onStatusChanged(InstallationStatus.INSTALLING);

        new Thread(() -> {
            PackageInstaller packageInstaller = mContext.getPackageManager().getPackageInstaller();
            int sessionID = -1;
            try {
                PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
                sessionID = packageInstaller.createSession(sessionParams);

                int finalSessionID = sessionID;
                mHandler.post(() -> setListener(finalSessionID, listener));

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
                int finalSessionID = sessionID;
                mHandler.post(() -> {
                    listener.onStatusChanged(InstallationStatus.INSTALLATION_FAILED);
                    removeListener(finalSessionID);
                });
            }
        }).start();
    }

    private void setListener(int sessionID, InstallationStatusListener listener) {
        mListeners.put(sessionID, listener);
    }

    private void removeListener(int sessionID) {
        mListeners.remove(sessionID);
    }

    private void dispatchSessionListenerStatusUpdate(int sessionID, InstallationStatus newStatus) {
        InstallationStatusListener listener = mListeners.get(sessionID);
        if (listener != null)
            listener.onStatusChanged(newStatus);
    }
}
