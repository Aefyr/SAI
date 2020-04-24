package com.aefyr.sai.utils;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Mirror of android.util.Log and some of the com.crashlytics.android.Crashlytics methods
 */
public class Logs {

    //Log.v
    public static int v(String tag, String message, Throwable tr) {
        return Log.v(tag, message, tr);
    }

    public static int v(String tag, String message) {
        return v(tag, message, null);
    }

    //Log.d
    public static int d(String tag, String message, Throwable tr) {
        return Log.d(tag, message, tr);
    }

    public static int d(String tag, String message) {
        return d(tag, message, null);
    }

    //Log.i
    public static int i(String tag, String message, Throwable tr) {
        return Log.i(tag, message, tr);
    }

    public static int i(String tag, String message) {
        return i(tag, message, null);
    }

    //Log.w
    public static int w(String tag, String message, Throwable tr) {
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
        return Log.e(tag, message, tr);
    }

    public static int e(String tag, String message) {
        return e(tag, message, null);
    }

    //Log.wtf
    public static int wtf(String tag, String message, Throwable tr) {
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
        //No Crashlytics in fdroid version, oof
        w("Oof", tr);
    }

    public static class InitProvider extends ContentProvider {

        @Override
        public boolean onCreate() {
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
