package com.aefyr.sai.firebase;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;

import io.fabric.sdk.android.services.common.DataCollectionArbiter;

public class Firebase {

    public static void setDataCollectionEnabled(Context context, boolean enabled) {
        Context appContext = context.getApplicationContext();

        FirebaseApp.getInstance().setDataCollectionDefaultEnabled(enabled);
        FirebaseAnalytics.getInstance(appContext).setAnalyticsCollectionEnabled(enabled);
        DataCollectionArbiter.getInstance(appContext).setCrashlyticsDataCollectionEnabled(enabled);
    }

}
