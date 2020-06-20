package com.aefyr.sai.analytics;

import android.content.Context;

import com.aefyr.sai.utils.PreferencesHelper;
import com.huawei.agconnect.crash.AGConnectCrash;
import com.huawei.hms.analytics.HiAnalytics;
import com.huawei.hms.analytics.HiAnalyticsInstance;

public class DefaultAnalyticsProvider implements AnalyticsProvider {

    private static DefaultAnalyticsProvider sInstance;

    private Context mContext;
    private PreferencesHelper mPrefsHelper;

    public static synchronized DefaultAnalyticsProvider getInstance(Context context) {
        return sInstance != null ? sInstance : new DefaultAnalyticsProvider(context);
    }

    private DefaultAnalyticsProvider(Context context) {
        mContext = context.getApplicationContext();
        mPrefsHelper = PreferencesHelper.getInstance(mContext);

        sInstance = this;
    }

    @Override
    public boolean supportsDataCollection() {
        return true;
    }

    @Override
    public boolean isDataCollectionEnabled() {
        return mPrefsHelper.isAnalyticsEnabled();
    }

    @Override
    public void setDataCollectionEnabled(boolean enabled) {

        HiAnalyticsInstance hiAnalytics = HiAnalytics.getInstance(mContext);
        hiAnalytics.clearCachedData();
        hiAnalytics.setAnalyticsEnabled(enabled);
        hiAnalytics.clearCachedData();

        AGConnectCrash agConnectCrash = AGConnectCrash.getInstance();
        agConnectCrash.enableCrashCollection(enabled);

        mPrefsHelper.setAnalyticsEnabled(enabled);
    }
}
