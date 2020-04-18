package com.aefyr.sai.installerx.resolver.meta;

import androidx.annotation.Nullable;

import com.aefyr.sai.installerx.common.SplitApkSourceMeta;

public class ApkSourceMetaResolutionResult {

    private boolean mSuccessful;
    private SplitApkSourceMeta mMeta;
    private ApkSourceMetaResolutionError mError;

    private ApkSourceMetaResolutionResult(boolean successful, @Nullable SplitApkSourceMeta meta, @Nullable ApkSourceMetaResolutionError error) {
        mSuccessful = successful;
        mMeta = meta;
        mError = error;
    }

    public static ApkSourceMetaResolutionResult success(SplitApkSourceMeta meta) {
        return new ApkSourceMetaResolutionResult(true, meta, null);
    }

    public static ApkSourceMetaResolutionResult failure(ApkSourceMetaResolutionError error) {
        return new ApkSourceMetaResolutionResult(false, null, error);
    }

    public boolean isSuccessful() {
        return mSuccessful;
    }

    public SplitApkSourceMeta meta() {
        return mMeta;
    }

    public ApkSourceMetaResolutionError error() {
        return mError;
    }

}
