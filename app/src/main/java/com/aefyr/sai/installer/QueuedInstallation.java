package com.aefyr.sai.installer;

import android.content.Context;

import com.aefyr.sai.model.apksource.ApkSource;

public class QueuedInstallation {

    private Context mContext;
    private ApkSource mApkSource;
    private long mId;

    QueuedInstallation(Context c, ApkSource apkSource, long id) {
        mContext = c;
        mApkSource = apkSource;
        mId = id;
    }

    public long getId() {
        return mId;
    }

    ApkSource getApkSource() {
        return mApkSource;
    }
}
