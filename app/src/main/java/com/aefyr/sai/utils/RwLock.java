package com.aefyr.sai.utils;

import java.util.concurrent.Callable;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RwLock {

    private ReadWriteLock mLock;

    public RwLock(ReadWriteLock backingLock) {
        mLock = backingLock;
    }

    public RwLock() {
        this(new ReentrantReadWriteLock(true));
    }

    public void withReadLock(Runnable action) {
        mLock.readLock().lock();
        try {
            action.run();
        } finally {
            mLock.readLock().unlock();
        }
    }

    public <T> T withReadLockReturn(Callable<T> callable) {
        mLock.readLock().lock();
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            mLock.readLock().unlock();
        }
    }

    public void withWriteLock(Runnable action) {
        mLock.writeLock().lock();
        try {
            action.run();
        } finally {
            mLock.writeLock().unlock();
        }
    }

    public <T> T withWriteLockReturn(Callable<T> callable) {
        mLock.writeLock().lock();
        try {
            return callable.call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            mLock.writeLock().unlock();
        }
    }


}
