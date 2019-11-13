package com.aefyr.sai.installer2.base.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SaiPiSessionState implements Comparable<SaiPiSessionState> {

    private String mSessionId;
    private SaiPiSessionStatus mStatus;
    private String mPackageName;
    private Exception mException;
    private long mLastUpdate;

    public SaiPiSessionState(String sessionId, SaiPiSessionStatus status, @Nullable String packageName, @Nullable Exception exception) {
        mSessionId = sessionId;
        mStatus = status;
        mPackageName = packageName;
        mException = exception;
        mLastUpdate = System.currentTimeMillis();
    }

    public SaiPiSessionState(String sessionId, SaiPiSessionStatus status, @Nullable String packageName) {
        this(sessionId, status, packageName, null);
    }

    public SaiPiSessionState(String sessionId, SaiPiSessionStatus status, @Nullable Exception exception) {
        this(sessionId, status, null, exception);
    }

    public SaiPiSessionState(String sessionId, SaiPiSessionStatus status) {
        this(sessionId, status, null, null);
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
}
