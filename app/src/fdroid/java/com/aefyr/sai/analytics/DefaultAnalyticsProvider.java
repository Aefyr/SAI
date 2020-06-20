package com.aefyr.sai.analytics;

import android.content.Context;

public class DefaultAnalyticsProvider implements AnalyticsProvider {

    private static DefaultAnalyticsProvider sInstance;

    private Context mContext;

    public static synchronized DefaultAnalyticsProvider getInstance(Context context) {
        return sInstance != null ? sInstance : new DefaultAnalyticsProvider(context);
    }

    private DefaultAnalyticsProvider(Context context) {
        mContext = context.getApplicationContext();

        sInstance = this;
    }

    @Override
    public boolean supportsDataCollection() {
        return false;
    }

    @Override
    public boolean isDataCollectionEnabled() {
        return false;
    }

    @Override
    public void setDataCollectionEnabled(boolean enabled) {

    }
}
