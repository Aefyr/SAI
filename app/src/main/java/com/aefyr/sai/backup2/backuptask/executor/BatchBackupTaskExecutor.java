package com.aefyr.sai.backup2.backuptask.executor;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;

import com.aefyr.sai.backup2.Backup;
import com.aefyr.sai.backup2.backuptask.config.BatchBackupTaskConfig;
import com.aefyr.sai.backup2.backuptask.config.SingleBackupTaskConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class BatchBackupTaskExecutor implements CancellableBackupTaskExecutor {

    private Context mContext;
    private BatchBackupTaskConfig mConfig;
    private SingleBackupTaskExecutorFactory mSingleBackupTaskExecutorFactory;

    private Stack<SingleBackupTaskConfig> mRemainingConfigs;

    private HandlerThread mWorkerHandlerThread;
    private Handler mWorkerHandler;
    private ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private Listener mListener;
    private Handler mListenerHandler;

    private AtomicBoolean mIsStarted = new AtomicBoolean(false);
    private AtomicBoolean mIsCancelled = new AtomicBoolean(false);

    public BatchBackupTaskExecutor(Context context, BatchBackupTaskConfig config, SingleBackupTaskExecutorFactory singleBackupTaskExecutorFactory) {
        mContext = context.getApplicationContext();
        mConfig = config;
        mSingleBackupTaskExecutorFactory = singleBackupTaskExecutorFactory;

        mRemainingConfigs = new Stack<>();
        mRemainingConfigs.addAll(mConfig.configs());
    }

    public void setListener(Listener listener, Handler listenerHandler) {
        mListener = listener;
        mListenerHandler = listenerHandler;
    }

    @Override
    public void requestCancellation() {
        mIsCancelled.set(true);
    }

    public void execute() {
        if (mIsStarted.getAndSet(true))
            throw new IllegalStateException("Unable to perform this action after execute has been called");

        mWorkerHandlerThread = new HandlerThread("BatchBackupTaskExecutor.Worker");
        mWorkerHandlerThread.start();
        mWorkerHandler = new Handler(mWorkerHandlerThread.getLooper());

        notifyStarted();
        mWorkerHandler.post(this::nextTask);
    }

    private void nextTask() {
        if (mIsCancelled.get()) {
            notifyCancelled(new ArrayList<>(mRemainingConfigs));
            cleanup();
            return;
        }

        if (mRemainingConfigs.empty()) {
            notifySucceeded();
            cleanup();
            return;
        }

        SingleBackupTaskConfig config = mRemainingConfigs.pop();
        SingleBackupTaskExecutor taskExecutor = mSingleBackupTaskExecutorFactory.createFor(config);
        taskExecutor.setListener(new SingleBackupTaskExecutor.Listener() {
            @Override
            public void onStart() {
                notifyAppBackupStarted(config);
            }

            @Override
            public void onProgressChanged(long current, long goal) {

            }

            @Override
            public void onCancelled() {
                nextTask();
            }

            @Override
            public void onSuccess(Backup backup) {
                notifyAppBackedUp(config, backup);
                nextTask();
            }

            @Override
            public void onError(Exception e) {
                notifyAppBackupFailed(config, e);
                nextTask();
            }
        }, mWorkerHandler);
        taskExecutor.execute(mExecutor);
    }

    private void cleanup() {
        mExecutor.shutdown();
        mWorkerHandlerThread.quitSafely();
    }

    private void notifyStarted() {
        if (mListener != null)
            mListenerHandler.post(() -> mListener.onStart());
    }

    private void notifyAppBackupStarted(SingleBackupTaskConfig config) {
        if (mListener != null)
            mListenerHandler.post(() -> mListener.onAppBackupStarted(config));
    }

    private void notifyAppBackedUp(SingleBackupTaskConfig config, Backup backup) {
        if (mListener != null)
            mListenerHandler.post(() -> mListener.onAppBackedUp(config, backup));
    }

    private void notifyAppBackupFailed(SingleBackupTaskConfig config, Exception e) {
        if (mListener != null)
            mListenerHandler.post(() -> mListener.onAppBackupFailed(config, e));
    }

    protected void notifyCancelled(List<SingleBackupTaskConfig> cancelledBackups) {
        if (mListener != null)
            mListenerHandler.post(() -> mListener.onCancelled(cancelledBackups));
    }

    private void notifySucceeded() {
        if (mListener != null)
            mListenerHandler.post(() -> mListener.onSuccess());
    }

    protected void notifyFailed(Exception e, List<SingleBackupTaskConfig> remainingBackups) {
        if (mListener != null)
            mListenerHandler.post(() -> mListener.onError(e, remainingBackups));
    }

    public interface Listener {

        void onStart();

        void onAppBackupStarted(SingleBackupTaskConfig config);

        void onAppBackedUp(SingleBackupTaskConfig config, Backup backup);

        void onAppBackupFailed(SingleBackupTaskConfig config, Exception e);

        void onCancelled(List<SingleBackupTaskConfig> cancelledBackups);

        void onSuccess();

        void onError(Exception e, List<SingleBackupTaskConfig> remainingBackups);

    }

    public interface SingleBackupTaskExecutorFactory {

        SingleBackupTaskExecutor createFor(SingleBackupTaskConfig config);

    }
}
