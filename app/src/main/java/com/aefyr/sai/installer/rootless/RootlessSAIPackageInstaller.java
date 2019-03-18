package com.aefyr.sai.installer.rootless;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.util.Log;

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
            switch (intent.getIntExtra(RootlessSAIPIService.EXTRA_INSTALLATION_STATUS, -1)) {
                case RootlessSAIPIService.STATUS_SUCCESS:
                    dispatchCurrentSessionUpdate(SAIPackageInstaller.InstallationStatus.INSTALLATION_SUCCEED, intent.getStringExtra(RootlessSAIPIService.EXTRA_PACKAGE_NAME));
                    installationCompleted();
                    break;
                case RootlessSAIPIService.STATUS_FAILURE:
                    dispatchCurrentSessionUpdate(SAIPackageInstaller.InstallationStatus.INSTALLATION_FAILED, intent.getStringExtra(RootlessSAIPIService.EXTRA_ERROR_DESCRIPTION));
                    installationCompleted();
                    break;
            }
        }
    };

    public static RootlessSAIPackageInstaller getInstance(Context c) {
        return sInstance != null ? sInstance : new RootlessSAIPackageInstaller(c);
    }

    private RootlessSAIPackageInstaller(Context c) {
        super(c);
        getContext().registerReceiver(mFurtherInstallationEventsReceiver, new IntentFilter(RootlessSAIPIService.ACTION_INSTALLATION_STATUS_NOTIFICATION));
        sInstance = this;
    }

    @Override
    protected void installApkFiles(ApkSource apkSource) {
        PackageInstaller packageInstaller = getContext().getPackageManager().getPackageInstaller();
        try {
            PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            int sessionID = packageInstaller.createSession(sessionParams);

            PackageInstaller.Session session = packageInstaller.openSession(sessionID);
            while (apkSource.nextApk()) {
                InputStream inputStream = apkSource.openApkInputStream();
                OutputStream outputStream = session.openWrite(apkSource.getApkName(), 0, apkSource.getApkLength());
                IOUtils.copyStream(inputStream, outputStream);
                session.fsync(outputStream);
                inputStream.close();
                outputStream.close();
            }

            Intent callbackIntent = new Intent(getContext(), RootlessSAIPIService.class);
            PendingIntent pendingIntent = PendingIntent.getService(getContext(), 0, callbackIntent, 0);
            session.commit(pendingIntent.getIntentSender());
            session.close();
        } catch (Exception e) {
            Log.w(TAG, e);
            dispatchCurrentSessionUpdate(SAIPackageInstaller.InstallationStatus.INSTALLATION_FAILED, getContext().getString(R.string.installer_error_rootless, e.getMessage()));
            installationCompleted();
        }
    }
}
