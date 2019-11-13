package com.aefyr.sai.installer2.impl.rootless;

import android.app.PendingIntent;
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

public class RootlessSaiPackageInstaller extends BaseSaiPackageInstaller implements RootlessSaiPiBroadcastReceiver.EventObserver {
    private static final String TAG = "RootlessSaiPi";

    private static RootlessSaiPackageInstaller sInstance;

    private PackageInstaller mPackageInstaller;
    private ExecutorService mExecutor = Executors.newFixedThreadPool(4);

    private ConcurrentHashMap<Integer, String> mAndroidPiSessionIdToSaiPiSessionId = new ConcurrentHashMap<>();

    private final RootlessSaiPiBroadcastReceiver mBroadcastReceiver;

    public static RootlessSaiPackageInstaller getInstance(Context c) {
        synchronized (RootlessSaiPackageInstaller.class) {
            return sInstance != null ? sInstance : new RootlessSaiPackageInstaller(c);
        }
    }

    private RootlessSaiPackageInstaller(Context c) {
        super(c);
        mPackageInstaller = getContext().getPackageManager().getPackageInstaller();

        mBroadcastReceiver = new RootlessSaiPiBroadcastReceiver(getContext());
        mBroadcastReceiver.addEventObserver(this);
        getContext().registerReceiver(mBroadcastReceiver, new IntentFilter(RootlessSaiPiBroadcastReceiver.ACTION_DELIVER_PI_EVENT));

        sInstance = this;
    }

    @Override
    public void enqueueSession(String sessionId) {
        SaiPiSessionParams params = takeCreatedSession(sessionId);
        setSessionState(sessionId, new SaiPiSessionState(sessionId, SaiPiSessionStatus.QUEUED));
        mExecutor.submit(() -> install(sessionId, params));
    }

    private void install(String sessionId, SaiPiSessionParams params) {
        setSessionState(sessionId, new SaiPiSessionState(sessionId, SaiPiSessionStatus.INSTALLING));
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

            Intent callbackIntent = new Intent(RootlessSaiPiBroadcastReceiver.ACTION_DELIVER_PI_EVENT);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 0, callbackIntent, 0);
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
    public void onInstallationSucceeded(int androidSessionId, String packageName) {
        String sessionId = mAndroidPiSessionIdToSaiPiSessionId.get(androidSessionId);
        if (sessionId == null)
            return;

        setSessionState(sessionId, new SaiPiSessionState(sessionId, SaiPiSessionStatus.INSTALLATION_SUCCEED, packageName));
    }

    @Override
    public void onInstallationFailed(int androidSessionId, Exception exception) {
        String sessionId = mAndroidPiSessionIdToSaiPiSessionId.get(androidSessionId);
        if (sessionId == null)
            return;

        //TODO do something about exception
        setSessionState(sessionId, new SaiPiSessionState(sessionId, SaiPiSessionStatus.INSTALLATION_FAILED, exception));

    }

    @Override
    protected String tag() {
        return TAG;
    }
}
