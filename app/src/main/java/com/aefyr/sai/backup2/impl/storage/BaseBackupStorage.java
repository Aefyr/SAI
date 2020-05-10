package com.aefyr.sai.backup2.impl.storage;

import android.os.Handler;

import androidx.core.util.Consumer;

import com.aefyr.sai.backup2.BackupFileMeta;
import com.aefyr.sai.backup2.BackupStorage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class BaseBackupStorage implements BackupStorage {

    private Map<BackupProgressListener, ProgressListenerHandlerWrapper> mProgressListeners = new ConcurrentHashMap<>();
    private Map<Observer, ObserverHandlerWrapper> mObservers = new ConcurrentHashMap<>();


    @Override
    public void addBackupProgressListener(BackupProgressListener progressListener, Handler progressListenerHandler) {
        mProgressListeners.put(progressListener, new ProgressListenerHandlerWrapper(progressListener, progressListenerHandler));
    }

    @Override
    public void removeBackupProgressListener(BackupProgressListener progressListener) {
        mProgressListeners.remove(progressListener);
    }

    @Override
    public void addObserver(Observer observer, Handler observerHandler) {
        mObservers.put(observer, new ObserverHandlerWrapper(observer, observerHandler));
    }

    @Override
    public void removeObserver(Observer observer) {
        mObservers.remove(observer);
    }

    protected void onEachProgressListener(Consumer<BackupProgressListener> action) {
        for (BackupProgressListener listener : mProgressListeners.values())
            action.accept(listener);
    }

    protected void notifyBackupTaskStatusChanged(BackupTaskStatus status) {
        onEachProgressListener(it -> it.onBackupTaskStatusChanged(status));
    }

    protected void notifyBatchBackupTaskStatusChanged(BatchBackupTaskStatus status) {
        onEachProgressListener(it -> it.onBatchBackupTaskStatusChanged(status));
    }

    protected void onEachObserver(Consumer<Observer> action) {
        for (Observer observer : mObservers.values())
            action.accept(observer);
    }

    protected void notifyBackupAdded(BackupFileMeta meta) {
        onEachObserver(it -> it.onBackupAdded(meta));
    }

    protected void notifyBackupRemoved(BackupFileMeta meta) {
        onEachObserver(it -> it.onBackupRemoved(meta));
    }

    protected void notifyStorageChanged() {
        onEachObserver(Observer::onStorageUpdated);
    }

    private static class ProgressListenerHandlerWrapper implements BackupProgressListener {

        private BackupProgressListener mWrappedListener;
        private Handler mHandler;

        private ProgressListenerHandlerWrapper(BackupProgressListener wrappedListener, Handler handler) {
            mWrappedListener = wrappedListener;
            mHandler = handler;
        }

        @Override
        public void onBackupTaskStatusChanged(BackupTaskStatus status) {
            mHandler.post(() -> mWrappedListener.onBackupTaskStatusChanged(status));
        }

        @Override
        public void onBatchBackupTaskStatusChanged(BatchBackupTaskStatus status) {
            mHandler.post(() -> mWrappedListener.onBatchBackupTaskStatusChanged(status));
        }
    }

    private static class ObserverHandlerWrapper implements Observer {

        private Observer mWrappedObserver;
        private Handler mHandler;

        private ObserverHandlerWrapper(Observer wrappedObserver, Handler handler) {
            mWrappedObserver = wrappedObserver;
            mHandler = handler;
        }


        @Override
        public void onBackupAdded(BackupFileMeta meta) {
            mHandler.post(() -> mWrappedObserver.onBackupAdded(meta));
        }

        @Override
        public void onBackupRemoved(BackupFileMeta meta) {
            mHandler.post(() -> mWrappedObserver.onBackupRemoved(meta));
        }

        @Override
        public void onStorageUpdated() {
            mHandler.post(() -> mWrappedObserver.onStorageUpdated());
        }
    }
}
