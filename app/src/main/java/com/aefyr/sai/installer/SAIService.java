package com.aefyr.sai.installer;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.os.IBinder;
import android.util.Log;

import com.aefyr.sai.R;
import com.aefyr.sai.utils.Utils;

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
    public static final String EXTRA_ERROR_DESCRIPTION = "com.aefyr.sai.extra.ERROR_DESCRIPTION";

    public static final int STATUS_SUCCESS = 0;
    public static final int STATUS_CONFIRMATION_PENDING = 1;
    public static final int STATUS_FAILURE = 2;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999);
        switch (intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999)) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                Log.d(TAG, "Requesting user confirmation for installation");
                sendStatusChangeBroadcast(intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1), STATUS_CONFIRMATION_PENDING, intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME));
                Intent confirmationIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);
                confirmationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                try {
                    startActivity(confirmationIntent);
                } catch (Exception e) {
                    sendErrorBroadcast(intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1), getString(R.string.installer_error_lidl_rom));
                }
                break;
            case PackageInstaller.STATUS_SUCCESS:
                Log.d(TAG, "Installation succeed");
                sendStatusChangeBroadcast(intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1), STATUS_SUCCESS, intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME));
                break;
            default:
                Log.d(TAG, "Installation failed");
                sendErrorBroadcast(intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1), getErrorString(status, intent.getStringExtra(PackageInstaller.EXTRA_OTHER_PACKAGE_NAME)));
                break;
        }
        stopSelf();
        return START_NOT_STICKY;
    }

    private void sendStatusChangeBroadcast(int sessionID, int status, String packageName) {
        Intent statusIntent = new Intent(ACTION_INSTALLATION_STATUS_NOTIFICATION);
        statusIntent.putExtra(EXTRA_INSTALLATION_STATUS, status);
        statusIntent.putExtra(EXTRA_SESSION_ID, sessionID);

        if (packageName != null)
            statusIntent.putExtra(EXTRA_PACKAGE_NAME, packageName);

        sendBroadcast(statusIntent);
    }

    private void sendErrorBroadcast(int sessionID, String error) {
        Intent statusIntent = new Intent(ACTION_INSTALLATION_STATUS_NOTIFICATION);
        statusIntent.putExtra(EXTRA_INSTALLATION_STATUS, STATUS_FAILURE);
        statusIntent.putExtra(EXTRA_SESSION_ID, sessionID);
        statusIntent.putExtra(EXTRA_ERROR_DESCRIPTION, error);

        sendBroadcast(statusIntent);
    }

    public String getErrorString(int status, String blockingPackage) {
        switch (status) {
            case PackageInstaller.STATUS_FAILURE_ABORTED:
                return getString(R.string.installer_error_aborted);

            case PackageInstaller.STATUS_FAILURE_BLOCKED:
                String blocker = getString(R.string.installer_error_blocked_device);
                if (blockingPackage != null) {
                    String appLabel = Utils.getAppLabel(getApplicationContext(), blockingPackage);
                    if (appLabel != null)
                        blocker = appLabel;
                }
                return getString(R.string.installer_error_blocked, blocker);

            case PackageInstaller.STATUS_FAILURE_CONFLICT:
                return getString(R.string.installer_error_conflict);

            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                return getString(R.string.installer_error_incompatible);

            case PackageInstaller.STATUS_FAILURE_INVALID:
                return getString(R.string.installer_error_bad_apks);

            case PackageInstaller.STATUS_FAILURE_STORAGE:
                return getString(R.string.installer_error_storage);
        }
        return getString(R.string.installer_error_generic);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
