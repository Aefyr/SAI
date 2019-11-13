package com.aefyr.sai.installer2.base.model;

import android.content.Context;

import com.aefyr.sai.R;

public enum SaiPiSessionStatus {
    CREATED, QUEUED, INSTALLING, INSTALLATION_SUCCEED, INSTALLATION_FAILED;

    public String getReadableName(Context c) {
        switch (this) {
            case CREATED:
                return c.getString(R.string.installer_state_created);
            case QUEUED:
                return c.getString(R.string.installer_state_queued);
            case INSTALLING:
                return c.getString(R.string.installer_state_installing);
            case INSTALLATION_SUCCEED:
                return c.getString(R.string.installer_state_installed);
            case INSTALLATION_FAILED:
                return c.getString(R.string.installer_state_failed);
        }

        throw new IllegalStateException("wtf");
    }
}
