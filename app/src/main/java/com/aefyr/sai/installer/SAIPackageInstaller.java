package com.aefyr.sai.installer;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.util.LongSparseArray;

import com.aefyr.sai.R;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.annotation.Nullable;

public abstract class SAIPackageInstaller {
    private static final String TAG = "SAIPI";

    public enum InstallationStatus {
        QUEUED, INSTALLING, INSTALLATION_SUCCEED, INSTALLATION_FAILED
    }

    private Context mContext;
    private Handler mHandler = new Handler(Looper.getMainLooper());
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();
    private ExecutorService mMiscExecutor = Executors.newSingleThreadExecutor();

    private ArrayDeque<QueuedInstallation> mInstallationQueue = new ArrayDeque<>();
    private ArrayList<InstallationStatusListener> mListeners = new ArrayList<>();
    private LongSparseArray<QueuedInstallation> mCreatedInstallationSessions = new LongSparseArray<>();

    private boolean mInstallationInProgress;
    private long mLastInstallationID = 0;
    private QueuedInstallation mOngoingInstallation;

    protected SAIPackageInstaller(Context c) {
        mContext = c.getApplicationContext();
    }

    protected Context getContext() {
        return mContext;
    }

    public interface InstallationStatusListener {
        void onStatusChanged(long installationID, InstallationStatus status, @Nullable String packageNameOrErrorDescription);
    }

    public void addStatusListener(InstallationStatusListener listener) {
        mListeners.add(listener);
    }

    public void removeStatusListener(InstallationStatusListener listener) {
        mListeners.remove(listener);
    }

    public long createInstallationSession(List<File> apkFiles) {
        long installationID = mLastInstallationID++;
        mCreatedInstallationSessions.put(installationID, new QueuedInstallation(getContext(), apkFiles, installationID));
        return installationID;
    }

    public long createInstallationSession(File zipWithApkFiles) {
        long installationID = mLastInstallationID++;
        mCreatedInstallationSessions.put(installationID, new QueuedInstallation(getContext(), zipWithApkFiles, installationID));
        return installationID;
    }

    public void startInstallationSession(long sessionID) {
        QueuedInstallation installation = mCreatedInstallationSessions.get(sessionID);
        mCreatedInstallationSessions.remove(sessionID);
        if (installation == null)
            return;

        mInstallationQueue.addLast(installation);
        dispatchSessionUpdate(installation.getId(), InstallationStatus.QUEUED, null);
        processQueue();
    }

    public boolean isInstallationInProgress() {
        return mInstallationInProgress;
    }

    private void processQueue() {
        if (mInstallationQueue.size() == 0 || mInstallationInProgress)
            return;

        QueuedInstallation installation = mInstallationQueue.removeFirst();
        mOngoingInstallation = installation;
        mInstallationInProgress = true;

        dispatchCurrentSessionUpdate(InstallationStatus.INSTALLING, null);

        mExecutor.execute(() -> {

            List<File> apkFiles;
            try {
                apkFiles = installation.getApkFiles();
            } catch (Exception e) {
                Log.w(TAG, e);
                dispatchCurrentSessionUpdate(InstallationStatus.INSTALLATION_FAILED, getContext().getString(R.string.installer_error_while_extracting, e.getMessage()));
                installationCompleted();
                return;
            }

            installApkFiles(apkFiles);
        });
    }

    protected abstract void installApkFiles(List<File> apkFiles);

    protected void installationCompleted() {
        mInstallationInProgress = false;
        QueuedInstallation lastInstallation = mOngoingInstallation;
        mMiscExecutor.submit(lastInstallation::clear);
        mOngoingInstallation = null;
        processQueue();
    }

    protected void dispatchSessionUpdate(long sessionID, InstallationStatus status, String packageNameOrError) {
        mHandler.post(() -> {
            for (InstallationStatusListener listener : mListeners)
                listener.onStatusChanged(sessionID, status, packageNameOrError);
        });
    }

    protected void dispatchCurrentSessionUpdate(InstallationStatus status, String packageNameOrError) {
        dispatchSessionUpdate(mOngoingInstallation.getId(), status, packageNameOrError);
    }
}
