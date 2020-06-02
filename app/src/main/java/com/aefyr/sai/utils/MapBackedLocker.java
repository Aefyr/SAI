package com.aefyr.sai.utils;

import android.annotation.TargetApi;
import android.os.Build;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

public abstract class MapBackedLocker<T> implements Locker<T> {

    public static <T> MapBackedLocker<T> create() {
        if (Utils.apiIsAtLeast(Build.VERSION_CODES.N))
            return new ConcurrentHashMapLocker<>();

        return new HashMapLocker<>();
    }

    private static class HashMapLocker<T> extends MapBackedLocker<T> {

        private final HashMap<T, Object> mLocks = new HashMap<>();

        private HashMapLocker() {

        }

        @Override
        public Object getLockFor(T t) {
            synchronized (mLocks) {
                Object lock = mLocks.get(t);
                if (lock == null) {
                    lock = new Object();
                    mLocks.put(t, lock);
                }

                return lock;
            }
        }

        @Override
        public void clearLock(T t) {
            synchronized (mLocks) {
                mLocks.remove(t);
            }
        }
    }

    @TargetApi(Build.VERSION_CODES.N)
    private static class ConcurrentHashMapLocker<T> extends MapBackedLocker<T> {

        private final ConcurrentHashMap<T, Object> mLocks = new ConcurrentHashMap<>();

        private ConcurrentHashMapLocker() {

        }

        @Override
        public Object getLockFor(T t) {
            return mLocks.computeIfAbsent(t, key -> new Object());
        }

        @Override
        public void clearLock(T t) {
            mLocks.remove(t);
        }
    }

}
