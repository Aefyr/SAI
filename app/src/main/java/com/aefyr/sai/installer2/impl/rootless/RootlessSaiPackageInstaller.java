package com.aefyr.sai.installer2.impl.rootless;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import com.aefyr.sai.installer2.base.model.SaiPiSessionParams;
import com.aefyr.sai.installer2.base.model.SaiPiSessionState;
import com.aefyr.sai.installer2.base.model.SaiPiSessionStatus;
import com.aefyr.sai.installer2.impl.BaseSaiPackageInstaller;
import com.aefyr.sai.model.apksource.ApkSource;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.PreferencesHelper;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RootlessSaiPackageInstaller extends BaseSaiPackageInstaller {
    private static final String TAG = "RootlessSaiPi";

    private static RootlessSaiPackageInstaller sInstance;

    private PackageInstaller mPackageInstaller;
    private ExecutorService mExecutor = Executors.newFixedThreadPool(4);

    private ConcurrentHashMap<Integer, String> mAndroidPiSessionIdToSaiPiSessionId = new ConcurrentHashMap<>();

    private final BroadcastReceiver mFurtherInstallationEventsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String sessionId = mAndroidPiSessionIdToSaiPiSessionId.get(intent.getIntExtra(RootlessSaiPiService.EXTRA_SESSION_ID, -1));
            if (sessionId == null)
                return;

            switch (intent.getIntExtra(RootlessSaiPiService.EXTRA_INSTALLATION_STATUS, -1)) {
                case RootlessSaiPiService.STATUS_SUCCESS:
                    setSessionState(sessionId, new SaiPiSessionState(sessionId, SaiPiSessionStatus.INSTALLATION_SUCCEED, intent.getStringExtra(RootlessSaiPiService.EXTRA_PACKAGE_NAME)));
                    break;
                case RootlessSaiPiService.STATUS_FAILURE:
                    //TODO do something about exception
                    setSessionState(sessionId, new SaiPiSessionState(sessionId, SaiPiSessionStatus.INSTALLATION_FAILED, new Exception(intent.getStringExtra(RootlessSaiPiService.EXTRA_ERROR_DESCRIPTION))));
                    break;
            }
        }
    };

    public static RootlessSaiPackageInstaller getInstance(Context c) {
        synchronized (RootlessSaiPackageInstaller.class) {
            return sInstance != null ? sInstance : new RootlessSaiPackageInstaller(c);
        }
    }

    private RootlessSaiPackageInstaller(Context c) {
        super(c);
        mPackageInstaller = getContext().getPackageManager().getPackageInstaller();
        getContext().registerReceiver(mFurtherInstallationEventsReceiver, new IntentFilter(RootlessSaiPiService.ACTION_INSTALLATION_STATUS_NOTIFICATION));
        sInstance = this;
    }

    @Override
    public void enqueueSession(String sessionId) {
        SaiPiSessionParams params = takeCreatedSession(sessionId);
        setSessionState(sessionId, new SaiPiSessionState(sessionId, SaiPiSessionStatus.QUEUED));
        mExecutor.submit(() -> install(sessionId, params));
    }

    private void install(String sessionId, SaiPiSessionParams params) {
        PackageInstaller.Session session = null;
        try (ApkSource apkSource = params.apkSource()) {
            PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            sessionParams.setInstallLocation(PreferencesHelper.getInstance(getContext()).getInstallLocation());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                sessionParams.setInstallReason(PackageManager.INSTALL_REASON_USER);

            int androidSessionId = mPackageInstaller.createSession(sessionParams);
            mAndroidPiSessionIdToSaiPiSessionId.put(androidSessionId, sessionId);

            session = mPackageInstaller.openSession(androidSessionId);
            int currentApkFile = 0;
            while (apkSource.nextApk()) {
                try (InputStream inputStream = apkSource.openApkInputStream(); OutputStream outputStream = session.openWrite(String.format("%d.apk", currentApkFile++), 0, apkSource.getApkLength())) {
                    IOUtils.copyStream(inputStream, outputStream);
                    session.fsync(outputStream);
                }
            }

            Intent callbackIntent = new Intent(getContext(), RootlessSaiPiService.class);
            PendingIntent pendingIntent = PendingIntent.getService(getContext(), 0, callbackIntent, 0);
            session.commit(pendingIntent.getIntentSender());
        } catch (Exception e) {
            Log.w(TAG, e);
            setSessionState(sessionId, new SaiPiSessionState(sessionId, SaiPiSessionStatus.INSTALLATION_FAILED, e));
        } finally {
            if (session != null)
                session.close();
        }
    }


    @Override
    protected String tag() {
        return TAG;
    }
}
