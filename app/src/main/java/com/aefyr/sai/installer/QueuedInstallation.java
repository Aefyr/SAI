package com.aefyr.sai.installer;

import android.content.Context;

import com.aefyr.sai.model.apksource.ApkSource;
import com.aefyr.sai.model.apksource.SignerApkSource;
import com.aefyr.sai.utils.PreferencesHelper;

class QueuedInstallation {

    private Context mContext;
    private ApkSource mApkSource;
    private long mId;

    QueuedInstallation(Context c, ApkSource apkSource, long id) {
        mContext = c;
        mApkSource = apkSource;
        mId = id;
    }

    long getId() {
        return mId;
    }

    ApkSource getApkSource() {
        if (PreferencesHelper.getInstance(mContext).shouldSignApks())
            return new SignerApkSource(mContext, mApkSource);

        return mApkSource;
    }
}
