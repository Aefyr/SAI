package com.aefyr.sai.legal;

import android.content.Context;

public class DefaultLegalStuffProvider implements LegalStuffProvider {

    private static DefaultLegalStuffProvider sInstance;

    private Context mContext;

    public static synchronized DefaultLegalStuffProvider getInstance(Context context) {
        return sInstance != null ? sInstance : new DefaultLegalStuffProvider(context);
    }

    private DefaultLegalStuffProvider(Context context) {
        mContext = context.getApplicationContext();

        sInstance = this;
    }

    @Override
    public boolean hasPrivacyPolicy() {
        return true;
    }

    @Override
    public String getPrivacyPolicyUrl() {
        return "https://aefyr.github.io/sai/privacy_en.html";
    }
}
