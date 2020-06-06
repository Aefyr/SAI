package com.aefyr.sai.utils;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

import java.util.Objects;

/**
 * Mirror of android.util.Log and some of the com.crashlytics.android.Crashlytics methods
 */
public class Logs {

    private static PreferencesHelper sPrefsHelper;
    private static FirebaseCrashlytics sCrashlytics;

    public static void init(Context context) {
        sPrefsHelper = PreferencesHelper.getInstance(context.getApplicationContext());
        sCrashlytics = FirebaseCrashlytics.getInstance();
    }

    //Log.v
    public static int v(String tag, String message, Throwable tr) {
        if (isCrashlyticsAvailable() && message != null)
            log(LogLevel.VERBOSE, tag, message);

        if (tr != null)
            logException(tr);

        return Log.v(tag, message, tr);
    }

    public static int v(String tag, String message) {
        return v(tag, message, null);
    }

    //Log.d
    public static int d(String tag, String message, Throwable tr) {
        if (isCrashlyticsAvailable() && message != null)
            log(LogLevel.DEBUG, tag, message);

        if (tr != null)
            logException(tr);

        return Log.d(tag, message, tr);
    }

    public static int d(String tag, String message) {
        return d(tag, message, null);
    }

    //Log.i
    public static int i(String tag, String message, Throwable tr) {
        if (isCrashlyticsAvailable() && message != null)
            log(LogLevel.INFO, tag, message);

        if (tr != null)
            logException(tr);

        return Log.i(tag, message, tr);
    }

    public static int i(String tag, String message) {
        return i(tag, message, null);
    }

    //Log.w
    public static int w(String tag, String message, Throwable tr) {
        if (isCrashlyticsAvailable() && message != null)
            log(LogLevel.WARN, tag, message);

        if (tr != null)
            logException(tr);

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
        if (isCrashlyticsAvailable() && message != null)
            log(LogLevel.ERROR, tag, message);

        if (tr != null)
            logException(tr);

        return Log.e(tag, message, tr);
    }

    public static int e(String tag, String message) {
        return e(tag, message, null);
    }

    //Log.wtf
    public static int wtf(String tag, String message, Throwable tr) {
        if (isCrashlyticsAvailable() && message != null)
            log(LogLevel.ASSERT, tag, message);

        if (tr != null)
            logException(tr);

        return Log.wtf(tag, message, tr);
    }

    public static int wtf(String tag, String message) {
        return wtf(tag, message, null);
    }

    public static int wtf(String tag, Throwable tr) {
        return wtf(tag, null, tr);
    }


    //Crashlytics
    private static void log(LogLevel level, String tag, String message) {
        sCrashlytics.log(String.format("%s/%s %s", level.format(), tag, message));
    }

    public static void logException(Throwable tr) {
        if (isCrashlyticsAvailable())
            sCrashlytics.recordException(tr);
    }

    private static boolean isCrashlyticsAvailable() {
        return sPrefsHelper.isFirebaseEnabled();
    }

    private enum LogLevel {
        VERBOSE("V"),
        DEBUG("D"),
        INFO("I"),
        WARN("W"),
        ERROR("E"),
        ASSERT("A");

        private String mName;

        LogLevel(String name) {
            mName = name;
        }

        private String format() {
            return mName;
        }

    }

    public static class InitProvider extends ContentProvider {

        @Override
        public boolean onCreate() {
            Logs.init(Objects.requireNonNull(getContext()));
            return true;
        }

        @Nullable
        @Override
        public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
            return null;
        }

        @Nullable
        @Override
        public String getType(@NonNull Uri uri) {
            return null;
        }

        @Nullable
        @Override
        public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
            return null;
        }

        @Override
        public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
            return 0;
        }

        @Override
        public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
            return 0;
        }
    }
}
