package com.aefyr.sai.installer2.impl.rootless;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;

import androidx.annotation.Nullable;

import com.aefyr.sai.BuildConfig;
import com.aefyr.sai.R;
import com.aefyr.sai.ui.activities.ConfirmationIntentWrapperActivity;
import com.aefyr.sai.utils.Utils;

import java.util.HashSet;

class RootlessSaiPiBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "RootlessSaiPiBR";

    public static final String ACTION_DELIVER_PI_EVENT = BuildConfig.APPLICATION_ID + ".action.RootlessSaiPiBroadcastReceiver.ACTION_DELIVER_PI_EVENT";

    private Context mContext;

    private HashSet<EventObserver> mObservers = new HashSet<>();

    public RootlessSaiPiBroadcastReceiver(Context c) {
        mContext = c.getApplicationContext();
    }

    public void addEventObserver(EventObserver observer) {
        mObservers.add(observer);
    }

    public void removeEventObserver(EventObserver observer) {
        mObservers.remove(observer);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999);
        switch (status) {
            case PackageInstaller.STATUS_PENDING_USER_ACTION:
                Log.d(TAG, "Requesting user confirmation for installation");
                dispatchOnConfirmationPending(intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1), intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME));
                Intent confirmationIntent = intent.getParcelableExtra(Intent.EXTRA_INTENT);

                //TODO !!! make a fixed ConfirmationIntentWrapperActivity cause this one will not work properly
                ConfirmationIntentWrapperActivity.start(context, intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1), confirmationIntent);
                break;
            case PackageInstaller.STATUS_SUCCESS:
                Log.d(TAG, "Installation succeed");
                dispatchOnInstallationSucceeded(intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1), intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME));
                break;
            default:
                Log.d(TAG, "Installation failed");
                dispatchOnInstallationFailed(intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1), new Exception(getErrorString(status, intent.getStringExtra(PackageInstaller.EXTRA_OTHER_PACKAGE_NAME))));
                break;
        }
    }

    private void dispatchOnConfirmationPending(int sessionId, @Nullable String packageName) {
        for (EventObserver observer : mObservers)
            observer.onConfirmationPending(sessionId, packageName);
    }

    private void dispatchOnInstallationSucceeded(int sessionId, String packageName) {
        for (EventObserver observer : mObservers)
            observer.onInstallationSucceeded(sessionId, packageName);
    }

    private void dispatchOnInstallationFailed(int sessionId, Exception exception) {
        for (EventObserver observer : mObservers)
            observer.onInstallationFailed(sessionId, exception);
    }

    public String getErrorString(int status, String blockingPackage) {
        switch (status) {
            case PackageInstaller.STATUS_FAILURE_ABORTED:
                return mContext.getString(R.string.installer_error_aborted);

            case PackageInstaller.STATUS_FAILURE_BLOCKED:
                String blocker = mContext.getString(R.string.installer_error_blocked_device);
                if (blockingPackage != null) {
                    String appLabel = Utils.getAppLabel(mContext, blockingPackage);
                    if (appLabel != null)
                        blocker = appLabel;
                }
                return mContext.getString(R.string.installer_error_blocked, blocker);

            case PackageInstaller.STATUS_FAILURE_CONFLICT:
                return mContext.getString(R.string.installer_error_conflict);

            case PackageInstaller.STATUS_FAILURE_INCOMPATIBLE:
                return mContext.getString(R.string.installer_error_incompatible);

            case PackageInstaller.STATUS_FAILURE_INVALID:
                return mContext.getString(R.string.installer_error_bad_apks);

            case PackageInstaller.STATUS_FAILURE_STORAGE:
                return mContext.getString(R.string.installer_error_storage);
        }
        return mContext.getString(R.string.installer_error_generic);
    }

    public interface EventObserver {

        default void onConfirmationPending(int sessionId, @Nullable String packageName) {

        }

        default void onInstallationSucceeded(int sessionId, String packageName) {

        }

        default void onInstallationFailed(int sessionId, Exception exception) {

        }
    }

}
