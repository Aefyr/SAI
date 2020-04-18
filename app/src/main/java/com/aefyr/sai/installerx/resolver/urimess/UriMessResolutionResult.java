package com.aefyr.sai.installerx.resolver.urimess;

import android.net.Uri;

import androidx.annotation.Nullable;

import com.aefyr.sai.installerx.common.SplitApkSourceMeta;

import java.util.List;

public class UriMessResolutionResult {

    private boolean mSuccessful;
    private SourceType mSourceType;
    private List<Uri> mUris;
    private SplitApkSourceMeta mMeta;
    private UriMessResolutionError mError;

    private UriMessResolutionResult(boolean successful, @Nullable SourceType sourceType, List<Uri> uris, @Nullable SplitApkSourceMeta meta, @Nullable UriMessResolutionError error) {
        mSuccessful = successful;
        mSourceType = sourceType;
        mUris = uris;
        mMeta = meta;
        mError = error;
    }

    public static UriMessResolutionResult success(SourceType sourceType, List<Uri> uris, SplitApkSourceMeta meta) {
        return new UriMessResolutionResult(true, sourceType, uris, meta, null);
    }

    public static UriMessResolutionResult failure(SourceType sourceType, List<Uri> uris, UriMessResolutionError error) {
        return new UriMessResolutionResult(false, sourceType, uris, null, error);
    }

    public boolean isSuccessful() {
        return mSuccessful;
    }

    public SourceType sourceType() {
        return mSourceType;
    }

    public List<Uri> uris() {
        return mUris;
    }

    public SplitApkSourceMeta meta() {
        return mMeta;
    }

    public UriMessResolutionError error() {
        return mError;
    }

}
