package com.aefyr.sai.installer2.impl.rootless;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import androidx.annotation.Nullable;

import com.aefyr.sai.installer2.base.model.SaiPiSessionParams;
import com.aefyr.sai.installer2.base.model.SaiPiSessionState;
import com.aefyr.sai.installer2.base.model.SaiPiSessionStatus;
import com.aefyr.sai.installer2.impl.BaseSaiPackageInstaller;
import com.aefyr.sai.model.apksource.ApkSource;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.utils.Utils;

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
    private final HandlerThread mWorkerThread = new HandlerThread("RootlessSaiPi Worker");
    private final Handler mWorkerHandler;

    private ConcurrentHashMap<Integer, String> mAndroidPiSessionIdToSaiPiSessionId = new ConcurrentHashMap<>();
    private ConcurrentHashMap<String, String> mSessionIdToAppTempName = new ConcurrentHashMap<>();

    private final RootlessSaiPiBroadcastReceiver mBroadcastReceiver;

    public static RootlessSaiPackageInstaller getInstance(Context c) {
        synchronized (RootlessSaiPackageInstaller.class) {
            return sInstance != null ? sInstance : new RootlessSaiPackageInstaller(c);
        }
    }

    private RootlessSaiPackageInstaller(Context c) {
        super(c);
        mPackageInstaller = getContext().getPackageManager().getPackageInstaller();

        mWorkerThread.start();
        mWorkerHandler = new Handler(mWorkerThread.getLooper());

        mBroadcastReceiver = new RootlessSaiPiBroadcastReceiver(getContext());
        mBroadcastReceiver.addEventObserver(this);
        getContext().registerReceiver(mBroadcastReceiver, new IntentFilter(RootlessSaiPiBroadcastReceiver.ACTION_DELIVER_PI_EVENT), null, mWorkerHandler);

        sInstance = this;
    }

    @Override
    public void enqueueSession(String sessionId) {
        SaiPiSessionParams params = takeCreatedSession(sessionId);
        setSessionState(sessionId, new SaiPiSessionState.Builder(sessionId, SaiPiSessionStatus.QUEUED).appTempName(params.apkSource().getAppName()).build());
        mExecutor.submit(() -> install(sessionId, params));
    }

    private void install(String sessionId, SaiPiSessionParams params) {
        PackageInstaller.Session session = null;
        String appTempName = null;
        try (ApkSource apkSource = params.apkSource()) {
            appTempName = apkSource.getAppName();
            if (appTempName != null)
                mSessionIdToAppTempName.put(sessionId, appTempName);

            setSessionState(sessionId, new SaiPiSessionState.Builder(sessionId, SaiPiSessionStatus.INSTALLING).appTempName(appTempName).build());

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
            if (session != null)
                session.abandon();

            setSessionState(sessionId, new SaiPiSessionState.Builder(sessionId, SaiPiSessionStatus.INSTALLATION_FAILED).appTempName(appTempName).error(e.getLocalizedMessage(), Utils.throwableToString(e)).build());
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

        setSessionState(sessionId, new SaiPiSessionState.Builder(sessionId, SaiPiSessionStatus.INSTALLATION_SUCCEED).packageName(packageName).resolvePackageMeta(getContext()).build());
    }

    @Override
    public void onInstallationFailed(int androidSessionId, String shortError, @Nullable String fullError, @Nullable Exception exception) {
        String sessionId = mAndroidPiSessionIdToSaiPiSessionId.get(androidSessionId);
        if (sessionId == null)
            return;

        setSessionState(sessionId, new SaiPiSessionState.Builder(sessionId, SaiPiSessionStatus.INSTALLATION_FAILED)
                .appTempName(mSessionIdToAppTempName.remove(sessionId))
                .error(shortError, fullError)
                .build());

    }

    @Override
    protected String tag() {
        return TAG;
    }
}
