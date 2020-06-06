package com.aefyr.sai.firebase;

import android.content.Context;

import com.google.firebase.FirebaseApp;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crashlytics.FirebaseCrashlytics;

public class Firebase {

    public static void setDataCollectionEnabled(Context context, boolean enabled) {
        Context appContext = context.getApplicationContext();

        FirebaseApp.getInstance().setDataCollectionDefaultEnabled(enabled);
        FirebaseAnalytics.getInstance(appContext).setAnalyticsCollectionEnabled(enabled);

        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.deleteUnsentReports();
        crashlytics.setCrashlyticsCollectionEnabled(enabled);
        crashlytics.deleteUnsentReports();
    }

}
