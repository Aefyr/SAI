package com.aefyr.sai.installer2.base.model;

import com.aefyr.sai.model.apksource.ApkSource;

public class SaiPiSessionParams {

    private ApkSource mApkSource;

    public SaiPiSessionParams(ApkSource apkSource) {
        mApkSource = apkSource;
    }

    public ApkSource apkSource() {
        return mApkSource;
    }

}
