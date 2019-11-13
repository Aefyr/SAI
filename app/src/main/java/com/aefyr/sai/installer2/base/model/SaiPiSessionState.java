package com.aefyr.sai.installer2.base.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class SaiPiSessionState {

    private String mSessionId;
    private SaiPiSessionStatus mStatus;
    private String mPackageName;
    private Exception mException;

    public SaiPiSessionState(String sessionId, SaiPiSessionStatus status, @Nullable String packageName, @Nullable Exception exception) {
        mSessionId = sessionId;
        mStatus = status;
        mPackageName = packageName;
        mException = exception;
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

    @NonNull
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("SaiPiSessionState: sessionId=%s, status=%s", sessionId(), status()));
        return sb.toString();
    }
}
