package com.aefyr.sai.backup2.backuptask.executor;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.os.Handler;

import com.aefyr.sai.backup2.Backup;
import com.aefyr.sai.backup2.backuptask.config.SingleBackupTaskConfig;

import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class SingleBackupTaskExecutor implements CancellableBackupTaskExecutor {

    private Context mContext;
    private SingleBackupTaskConfig mConfig;
    private DelegatedFile mDelegatedFile;

    private Listener mListener;
    private Handler mListenerHandler;

    private AtomicBoolean mIsStarted = new AtomicBoolean(false);
    private AtomicBoolean mIsCancelled = new AtomicBoolean(false);

    public SingleBackupTaskExecutor(Context context, SingleBackupTaskConfig config, DelegatedFile delegatedFile) {
        mContext = context.getApplicationContext();
        mConfig = config;
        mDelegatedFile = delegatedFile;
    }

    public void setListener(Listener listener, Handler listenerHandler) {
        ensureNotStarted();

        mListener = listener;
        mListenerHandler = listenerHandler;
    }

    public boolean isStarted() {
        return mIsStarted.get();
    }

    public boolean isCancelled() {
        return mIsCancelled.get();
    }

    @Override
    public void requestCancellation() {
        mIsCancelled.set(true);
    }

    public void execute(Executor executor) {
        if (mIsStarted.getAndSet(true)) {
            throw new IllegalStateException("Unable to call this method after execution has been started");
        }

        executor.execute(this::executeInternal);
    }

    protected abstract void executeInternal();

    public Context getContext() {
        return mContext;
    }

    public SingleBackupTaskConfig getConfig() {
        return mConfig;
    }

    public DelegatedFile getFile() {
        return mDelegatedFile;
    }

    protected void ensureNotStarted() {
        if (mIsStarted.get())
            throw new IllegalStateException("Unable to call this method after execution has been started");
    }

    protected void ensureNotCancelled() throws TaskCancelledException {
        if (isCancelled())
            throw new TaskCancelledException();
    }

    protected List<File> getAllApkFilesForPackage(String pkg) throws Exception {
        ApplicationInfo applicationInfo = mContext.getPackageManager().getApplicationInfo(pkg, 0);

        List<File> apkFiles = new ArrayList<>();
        apkFiles.add(new File(applicationInfo.publicSourceDir));

        if (applicationInfo.splitPublicSourceDirs != null) {
            for (String splitPath : applicationInfo.splitPublicSourceDirs)
                apkFiles.add(new File(splitPath));
        }

        return apkFiles;
    }

    protected void notifyStarted() {
        if (mListener != null)
            mListenerHandler.post(() -> mListener.onStart());
    }

    protected void notifyProgressChanged(long current, long goal) {
        if (mListener != null)
            mListenerHandler.post(() -> mListener.onProgressChanged(current, goal));
    }

    protected void notifyCancelled() {
        if (mListener != null)
            mListenerHandler.post(() -> mListener.onCancelled());
    }

    protected void notifySucceeded(Backup backup) {
        if (mListener != null)
            mListenerHandler.post(() -> mListener.onSuccess(backup));
    }

    protected void notifyFailed(Exception e) {
        if (mListener != null)
            mListenerHandler.post(() -> mListener.onError(e));
    }

    public interface DelegatedFile {

        OutputStream openOutputStream() throws Exception;

        void delete();

        Backup readMeta() throws Exception;

    }

    public interface Listener {

        void onStart();

        void onProgressChanged(long current, long goal);

        void onCancelled();

        void onSuccess(Backup backup);

        void onError(Exception e);

    }

    protected static class TaskCancelledException extends Exception {

    }

}
