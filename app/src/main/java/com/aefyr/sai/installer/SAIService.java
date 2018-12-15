package com.aefyr.sai.installer;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * Handles installation events from the package manager
 */
public class SAIService extends Service {
    private static final String TAG = "SAIService";

    public static final String ACTION_INSTALLATION_STATUS_NOTIFICATION = "com.aefyr.sai.action.INSTALLATION_STATUS_NOTIFICATION";
    public static final String EXTRA_INSTALLATION_STATUS = "com.aefyr.sai.extra.INSTALLATION_STATUS";
    public static final String EXTRA_SESSION_ID = "com.aefyr.sai.extra.SESSION_ID";
    public static final String EXTRA_PACKAGE_NAME = "com.aefyr.sai.extra.PACKAGE_NAME";

    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_CONFIRMATION_PENDING = 1;
    public static final int STATUS_FAILURE = 2;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        switch (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999)) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                Log.d(TAG, "Requesting user confirmation for installation");
                setStatusChangeBroadcast(intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1), STATUS_CONFIRMATION_PENDING, intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME));
                Intent confirmationIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(confirmationIntent);
                break;
            case PackageInstaller.STATUS_SUCCESS:
                Log.d(TAG, "Installation succeed");
                setStatusChangeBroadcast(intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1), STATUS_SUCCESS, intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME));
                break;
            default:
                Log.d(TAG, "Installation failed");
                setStatusChangeBroadcast(intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1), STATUS_FAILURE, intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME));
                break;
        }
        stopSelf();
        return START_NOT_STICKY;
    }

    private void setStatusChangeBroadcast(int sessionID, int status, String packageName) {
        Intent statusIntent = new Intent(ACTION_INSTALLATION_STATUS_NOTIFICATION);
        statusIntent.putExtra(EXTRA_INSTALLATION_STATUS, status);
        statusIntent.putExtra(EXTRA_SESSION_ID, sessionID);

        if (packageName != null)
            statusIntent.putExtra(EXTRA_PACKAGE_NAME, packageName);

        sendBroadcast(statusIntent);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
