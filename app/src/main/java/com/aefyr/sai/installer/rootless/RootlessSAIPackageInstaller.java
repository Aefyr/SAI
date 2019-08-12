package com.aefyr.sai.installer.rootless;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.util.Log;
import android.util.SparseLongArray;

import com.aefyr.sai.R;
import com.aefyr.sai.installer.SAIPackageInstaller;
import com.aefyr.sai.model.apksource.ApkSource;
import com.aefyr.sai.utils.IOUtils;

import java.io.InputStream;
import java.io.OutputStream;

public class RootlessSAIPackageInstaller extends SAIPackageInstaller {
    private static final String TAG = "RootlessSAIPI";

    @SuppressLint("StaticFieldLeak")//This is application context, lul
    private static RootlessSAIPackageInstaller sInstance;

    private BroadcastReceiver mFurtherInstallationEventsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long sessionId = mSessionsMap.get(intent.getIntExtra(RootlessSAIPIService.EXTRA_SESSION_ID, -1), -1);
            if (sessionId == -1)
                return;
            switch (intent.getIntExtra(RootlessSAIPIService.EXTRA_INSTALLATION_STATUS, -1)) {
                case RootlessSAIPIService.STATUS_SUCCESS:
                    dispatchSessionUpdate(sessionId, SAIPackageInstaller.InstallationStatus.INSTALLATION_SUCCEED, intent.getStringExtra(RootlessSAIPIService.EXTRA_PACKAGE_NAME));
                    if (getOngoingInstallation() != null && sessionId == getOngoingInstallation().getId())
                        installationCompleted();
                    break;
                case RootlessSAIPIService.STATUS_FAILURE:
                    dispatchSessionUpdate(sessionId, SAIPackageInstaller.InstallationStatus.INSTALLATION_FAILED, intent.getStringExtra(RootlessSAIPIService.EXTRA_ERROR_DESCRIPTION));
                    if (getOngoingInstallation() != null && sessionId == getOngoingInstallation().getId())
                        installationCompleted();
                    break;
            }
        }
    };

    private PackageInstaller mPackageInstaller;

    /**
     * Maps Android PackageInstaller session id to SAIPackageInstaller QueuedInstallation id
     */
    private SparseLongArray mSessionsMap = new SparseLongArray();


    public static RootlessSAIPackageInstaller getInstance(Context c) {
        return sInstance != null ? sInstance : new RootlessSAIPackageInstaller(c);
    }

    private RootlessSAIPackageInstaller(Context c) {
        super(c);
        mPackageInstaller = getContext().getPackageManager().getPackageInstaller();
        getContext().registerReceiver(mFurtherInstallationEventsReceiver, new IntentFilter(RootlessSAIPIService.ACTION_INSTALLATION_STATUS_NOTIFICATION));
        sInstance = this;
    }

    @Override
    protected void installApkFiles(ApkSource apkSource) {
        cleanOldSessions();

        PackageInstaller.Session session = null;
        try {
            Thread.sleep(5000);
            PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            sessionParams.setInstallLocation(PackageInfo.INSTALL_LOCATION_AUTO);

            int sessionID = mPackageInstaller.createSession(sessionParams);
            mSessionsMap.put(sessionID, getOngoingInstallation().getId());

            session = mPackageInstaller.openSession(sessionID);
            while (apkSource.nextApk()) {
                try (InputStream inputStream = apkSource.openApkInputStream(); OutputStream outputStream = session.openWrite(apkSource.getApkName(), 0, apkSource.getApkLength())) {
                    IOUtils.copyStream(inputStream, outputStream);
                    session.fsync(outputStream);
                }
            }

            Intent callbackIntent = new Intent(getContext(), RootlessSAIPIService.class);
            PendingIntent pendingIntent = PendingIntent.getService(getContext(), 0, callbackIntent, 0);
            session.commit(pendingIntent.getIntentSender());
        } catch (Exception e) {
            Log.w(TAG, e);
            dispatchCurrentSessionUpdate(SAIPackageInstaller.InstallationStatus.INSTALLATION_FAILED, getContext().getString(R.string.installer_error_rootless, e.getMessage()));
            installationCompleted();
        } finally {
            if (session != null)
                session.close();
        }
    }

    private void cleanOldSessions() {
        int cleanedSessions = 0;
        long start = System.currentTimeMillis();

        for (PackageInstaller.SessionInfo sessionInfo : mPackageInstaller.getMySessions()) {
            try {
                mPackageInstaller.abandonSession(sessionInfo.getSessionId());
                cleanedSessions++;
            } catch (Exception e) {
                Log.w(TAG, "Unable to abandon session", e);
            }
        }

        Log.d(TAG, String.format("Cleaned %d sessions in %d ms.", cleanedSessions, (System.currentTimeMillis() - start)));
    }
}
