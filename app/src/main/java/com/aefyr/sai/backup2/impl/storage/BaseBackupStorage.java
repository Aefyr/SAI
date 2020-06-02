package com.aefyr.sai.backup2.impl.storage;

import android.net.Uri;
import android.os.Handler;

import androidx.core.util.Consumer;

import com.aefyr.sai.backup2.Backup;
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
        onEachProgressListener(it -> it.onBackupTaskStatusChanged(getStorageId(), status));
    }

    protected void notifyBatchBackupTaskStatusChanged(BatchBackupTaskStatus status) {
        onEachProgressListener(it -> it.onBatchBackupTaskStatusChanged(getStorageId(), status));
    }

    protected void onEachObserver(Consumer<Observer> action) {
        for (Observer observer : mObservers.values())
            action.accept(observer);
    }

    protected void notifyBackupAdded(Backup backup) {
        onEachObserver(it -> it.onBackupAdded(getStorageId(), backup));
    }

    protected void notifyBackupRemoved(Uri backupUri) {
        onEachObserver(it -> it.onBackupRemoved(getStorageId(), backupUri));
    }

    protected void notifyStorageChanged() {
        onEachObserver(it -> it.onStorageUpdated(getStorageId()));
    }

    private static class ProgressListenerHandlerWrapper implements BackupProgressListener {

        private BackupProgressListener mWrappedListener;
        private Handler mHandler;

        private ProgressListenerHandlerWrapper(BackupProgressListener wrappedListener, Handler handler) {
            mWrappedListener = wrappedListener;
            mHandler = handler;
        }

        @Override
        public void onBackupTaskStatusChanged(String storageId, BackupTaskStatus status) {
            mHandler.post(() -> mWrappedListener.onBackupTaskStatusChanged(storageId, status));
        }

        @Override
        public void onBatchBackupTaskStatusChanged(String storageId, BatchBackupTaskStatus status) {
            mHandler.post(() -> mWrappedListener.onBatchBackupTaskStatusChanged(storageId, status));
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
        public void onBackupAdded(String storageId, Backup backup) {
            mHandler.post(() -> mWrappedObserver.onBackupAdded(storageId, backup));
        }

        @Override
        public void onBackupRemoved(String storageId, Uri backupUri) {
            mHandler.post(() -> mWrappedObserver.onBackupRemoved(storageId, backupUri));
        }

        @Override
        public void onStorageUpdated(String storageId) {
            mHandler.post(() -> mWrappedObserver.onStorageUpdated(storageId));
        }
    }
}
