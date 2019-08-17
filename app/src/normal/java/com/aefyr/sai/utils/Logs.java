package com.aefyr.sai.utils;

import android.util.Log;

import com.crashlytics.android.Crashlytics;

/**
 * Mirror of android.util.Log and some of the com.crashlytics.android.Crashlytics methods
 */
public class Logs {

    //Log.v
    public static int v(String tag, String message, Throwable tr) {
        Crashlytics.log(Log.VERBOSE, tag, message);
        return Log.v(tag, message, tr);
    }

    public static int v(String tag, String message) {
        return v(tag, message, null);
    }

    //Log.d
    public static int d(String tag, String message, Throwable tr) {
        Crashlytics.log(Log.DEBUG, tag, message);
        return Log.d(tag, message, tr);
    }

    public static int d(String tag, String message) {
        return d(tag, message, null);
    }

    //Log.i
    public static int i(String tag, String message, Throwable tr) {
        Crashlytics.log(Log.INFO, tag, message);
        return Log.i(tag, message, tr);
    }

    public static int i(String tag, String message) {
        return i(tag, message, null);
    }

    //Log.w
    public static int w(String tag, String message, Throwable tr) {
        if (message != null)
            Crashlytics.log(Log.WARN, tag, message);
        return Log.w(tag, message, tr);
    }

    public static int w(String tag, String message) {
        return w(tag, message, null);
    }

    public static int w(String tag, Throwable tr) {
        return w(tag, null, tr);
    }

    //Log.e
    public static int e(String tag, String message, Throwable tr) {
        if (message != null)
            Crashlytics.log(Log.ERROR, tag, message);
        return Log.e(tag, message, tr);
    }

    public static int e(String tag, String message) {
        return e(tag, message, null);
    }

    //Log.wtf
    public static int wtf(String tag, String message, Throwable tr) {
        if (message != null)
            Crashlytics.log(Log.ERROR, tag, message);
        return Log.wtf(tag, message, tr);
    }

    public static int wtf(String tag, String message) {
        return wtf(tag, message, null);
    }

    public static int wtf(String tag, Throwable tr) {
        return wtf(tag, null, tr);
    }

    //Crashlytics
    public static void logException(Throwable tr) {
        Crashlytics.logException(tr);
    }
}
