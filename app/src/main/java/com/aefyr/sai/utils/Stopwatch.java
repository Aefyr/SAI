package com.aefyr.sai.utils;

public class Stopwatch {

    private long mStart;

    public Stopwatch() {
        mStart = System.currentTimeMillis();
    }

    public long millisSinceStart() {
        return System.currentTimeMillis() - mStart;
    }
}
