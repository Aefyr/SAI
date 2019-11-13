package com.aefyr.sai.installer2.base;

import com.aefyr.sai.installer2.base.model.SaiPiSessionState;

public interface SaiPiSessionObserver {

    void onSessionStateChanged(SaiPiSessionState state);

}
