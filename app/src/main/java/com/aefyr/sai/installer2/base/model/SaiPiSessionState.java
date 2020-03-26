package com.aefyr.sai.installer2.base.model;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aefyr.sai.model.common.PackageMeta;
import com.aefyr.sai.utils.Stopwatch;

public class SaiPiSessionState implements Comparable<SaiPiSessionState> {

    private String mSessionId;
    private SaiPiSessionStatus mStatus;
    private String mPackageName;
    private String mAppTempName;
    private PackageMeta mPackageMeta;
    private long mLastUpdate;
    private String mShortError;
    private String mFullError;

    private SaiPiSessionState(String sessionId, SaiPiSessionStatus status) {
        mSessionId = sessionId;
        mStatus = status;
        mLastUpdate = System.currentTimeMillis();
    }

    public String sessionId() {
        return mSessionId;
    }

    public SaiPiSessionStatus status() {
        return mStatus;
    }

    @Nullable
    public String packageName() {
        return mPackageName;
    }

    @Nullable
    public String appTempName() {
        if (mAppTempName != null)
            return mAppTempName;

        if (mPackageName != null)
            return mPackageName;

        return null;
    }

    @Nullable
    public PackageMeta packageMeta() {
        return mPackageMeta;
    }

    /**
     * @return user-readable error description
     */
    @Nullable
    public String shortError() {
        return mShortError;
    }

    /**
     * @return full error info for debugging and stuff. May be same as {@link #shortError()} if there's no better info
     */
    @Nullable
    public String fullError() {
        return mFullError;
    }

    public long lastUpdate() {
        return mLastUpdate;
    }

    public Builder newBuilder() {
        return new Builder(mSessionId, mStatus)
                .packageName(packageName())
                .appTempName(appTempName())
                .packageMeta(packageMeta())
                .error(shortError(), fullError());
    }

    @Override
    public int hashCode() {
        return sessionId().hashCode();
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof SaiPiSessionState && ((SaiPiSessionState) obj).sessionId().equals(sessionId());
    }

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("SaiPiSessionState: sessionId=%s, status=%s", sessionId(), status()));
        return sb.toString();
    }

    @Override
    public int compareTo(SaiPiSessionState o) {
        return Long.compare(o.lastUpdate(), lastUpdate());
    }

    public static class Builder {
        private SaiPiSessionState mState;

        public Builder(@NonNull String sessionId, @NonNull SaiPiSessionStatus status) {
            mState = new SaiPiSessionState(sessionId, status);
        }

        public Builder packageName(@Nullable String packageName) {
            mState.mPackageName = packageName;
            return this;
        }

        public Builder appTempName(@Nullable String tempAppName) {
            mState.mAppTempName = tempAppName;
            return this;
        }

        public Builder resolvePackageMeta(Context c) {
            if (mState.mPackageName == null)
                return this;

            Stopwatch sw = new Stopwatch();
            mState.mPackageMeta = PackageMeta.forPackage(c, mState.mPackageName);
            Log.d("SaiPiSessionState", String.format("Got PackageMeta in %d ms.", sw.millisSinceStart()));
            return this;
        }

        public Builder packageMeta(@Nullable PackageMeta packageMeta) {
            mState.mPackageMeta = packageMeta;
            return this;
        }

        public Builder error(String shortError, @Nullable String fullError) {
            mState.mShortError = shortError;
            if (fullError == null)
                fullError = shortError;

            mState.mFullError = fullError;
            return this;
        }

        public SaiPiSessionState build() {
            mState.mLastUpdate = System.currentTimeMillis();
            return mState;
        }
    }
}
