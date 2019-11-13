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
    private PackageMeta mPackageMeta;
    private Exception mException;
    private long mLastUpdate;

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
    public PackageMeta packageMeta() {
        return mPackageMeta;
    }

    @Nullable
    public Exception exception() {
        return mException;
    }

    public long lastUpdate() {
        return mLastUpdate;
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

        public Builder(String sessionId, SaiPiSessionStatus status) {
            mState = new SaiPiSessionState(sessionId, status);
        }

        public Builder packageName(String packageName) {
            mState.mPackageName = packageName;
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

        public Builder exception(Exception exception) {
            mState.mException = exception;
            return this;
        }

        public SaiPiSessionState build() {
            return mState;
        }
    }
}
