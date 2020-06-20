package com.aefyr.sai.flavor;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aefyr.sai.R;
import com.huawei.agconnect.config.AGConnectServicesConfig;
import com.huawei.agconnect.config.LazyInputStream;

import java.io.InputStream;
import java.util.Objects;

public class FlavorInitProvider extends ContentProvider {

    @NonNull
    protected Context requireContext() {
        return Objects.requireNonNull(getContext(), "context is null");
    }

    @Override
    public boolean onCreate() {
        initAGConnect();
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

    private void initAGConnect() {
        AGConnectServicesConfig config = AGConnectServicesConfig.fromContext(requireContext());
        config.overlayWith(new LazyInputStream(requireContext()) {
            @Override
            public InputStream get(Context context) {
                return context.getResources().openRawResource(R.raw.agconfig);
            }
        });
    }
}
