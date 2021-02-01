package com.aefyr.sai.shizuku;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aefyr.sai.BuildConfig;
import com.aefyr.sai.utils.Utils;

import rikka.sui.Sui;

public class SuiInitProvider extends ContentProvider {
    public static final String TAG = "SuiInitProvider";

    @Override
    public boolean onCreate() {
        if (Utils.apiIsAtLeast(Build.VERSION_CODES.M)) {
            Log.d(TAG, String.format("Sui.init = %s", Sui.init(BuildConfig.APPLICATION_ID)));
        }

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
