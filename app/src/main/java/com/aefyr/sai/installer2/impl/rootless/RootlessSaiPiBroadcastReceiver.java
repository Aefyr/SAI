package com.aefyr.sai.installer2.impl.rootless;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInstaller;
import android.util.Log;

import androidx.annotation.Nullable;

import com.aefyr.sai.BuildConfig;
import com.aefyr.sai.R;
import com.aefyr.sai.installer2.base.model.AndroidPackageInstallerError;
import com.aefyr.sai.utils.Utils;

import java.util.HashSet;

class RootlessSaiPiBroadcastReceiver extends BroadcastReceiver {
    private static final String TAG = "RootlessSaiPiBR";

    public static final String ANDROID_PM_EXTRA_LEGACY_STATUS = "android.content.pm.extra.LEGACY_STATUS";

    public static final String ACTION_DELIVER_PI_EVENT = BuildConfig.APPLICATION_ID + ".action.RootlessSaiPiBroadcastReceiver.ACTION_DELIVER_PI_EVENT";

    public static final int STATUS_BAD_ROM = -322;

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

                ConfirmationIntentWrapperActivity2.start(context, intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1), confirmationIntent);
                break;
            case PackageInstaller.STATUS_SUCCESS:
                Log.d(TAG, "Installation succeed");
                dispatchOnInstallationSucceeded(intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1), intent.getStringExtra(PackageInstaller.EXTRA_PACKAGE_NAME));
                break;
            default:
                Log.d(TAG, "Installation failed");
                dispatchOnInstallationFailed(intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1), parseError(intent), getRawError(intent), null);
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

    private void dispatchOnInstallationFailed(int sessionId, String shortError, @Nullable String fullError, @Nullable Exception exception) {
        for (EventObserver observer : mObservers)
            observer.onInstallationFailed(sessionId, shortError, fullError, exception);
    }

    @Nullable
    private String getRawError(Intent intent) {
        return intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
    }

    private String parseError(Intent intent) {
        int status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, -999);
        String otherPackage = intent.getStringExtra(PackageInstaller.EXTRA_OTHER_PACKAGE_NAME);
        String error = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE);
        int errorCode = intent.getIntExtra(ANDROID_PM_EXTRA_LEGACY_STATUS, AndroidPackageInstallerError.UNKNOWN.getLegacyErrorCode());

        if (status == STATUS_BAD_ROM) {
            return mContext.getString(R.string.installer_error_lidl_rom);
        }

        AndroidPackageInstallerError androidPackageInstallerError = getAndroidPmError(errorCode, error);
        if (androidPackageInstallerError != AndroidPackageInstallerError.UNKNOWN) {
            return androidPackageInstallerError.getDescription(mContext);
        }

        return getSimplifiedErrorDescription(status, otherPackage);
    }

    public String getSimplifiedErrorDescription(int status, String blockingPackage) {
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

            case STATUS_BAD_ROM:
                return mContext.getString(R.string.installer_error_lidl_rom);
        }
        return mContext.getString(R.string.installer_error_generic);
    }

    public AndroidPackageInstallerError getAndroidPmError(int legacyErrorCode, @Nullable String error) {
        for (AndroidPackageInstallerError androidPackageInstallerError : AndroidPackageInstallerError.values()) {
            if (androidPackageInstallerError.getLegacyErrorCode() == legacyErrorCode || (error != null && error.startsWith(androidPackageInstallerError.getError())))
                return androidPackageInstallerError;
        }
        return AndroidPackageInstallerError.UNKNOWN;
    }

    public interface EventObserver {

        default void onConfirmationPending(int sessionId, @Nullable String packageName) {

        }

        default void onInstallationSucceeded(int sessionId, String packageName) {

        }

        default void onInstallationFailed(int sessionId, String shortError, @Nullable String fullError, @Nullable Exception exception) {

        }
    }

}
