package com.aefyr.sai.utils;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class SimpleAsyncTask<Argument, Result> {

    private static ExecutorService sExecutor = Executors.newCachedThreadPool();
    private static Handler sHandler = new Handler(Looper.getMainLooper());

    private Argument mArgument;
    private AtomicBoolean mIsCancelled = new AtomicBoolean();
    private AtomicBoolean mIsOngoing = new AtomicBoolean();

    public SimpleAsyncTask(Argument argument) {
        mArgument = argument;
    }

    /**
     * If invoked on main thread, guarantees that onWorkDone and onError will never be called
     */
    public final void cancel() {
        mIsCancelled.set(true);
    }

    public final boolean isCancelled() {
        return mIsCancelled.get();
    }

    public final boolean isOngoing() {
        return mIsOngoing.get();
    }

    public final <T extends SimpleAsyncTask<Argument, Result>> T execute() {
        if (isOngoing())
            throw new IllegalStateException("Unable to execute a task that is already ongoing");

        mIsOngoing.set(true);
        sExecutor.submit(() -> {
            try {
                Result result = doWork(mArgument);
                sHandler.post(() -> {
                    if (isCancelled()) {
                        mIsOngoing.set(false);
                        return;
                    }

                    onWorkDone(result);
                    mIsOngoing.set(false);
                });
            } catch (Exception e) {
                sHandler.post(() -> {
                    if (isCancelled()) {
                        mIsOngoing.set(false);
                        return;
                    }

                    onError(e);
                    mIsOngoing.set(false);
                });
            }
        });

        return (T) this;
    }

    protected abstract Result doWork(Argument argument) throws Exception;

    protected abstract void onWorkDone(Result result);

    protected abstract void onError(Exception exception);

}