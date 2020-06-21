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
        return false;
    }

    @Override
    public String getPrivacyPolicyUrl() {
        return null;
    }

    @Override
    public boolean hasEula() {
        return true;
    }

    @Override
    public String getEulaUrl() {
        return "https://aefyr.github.io/sai/eula_en";
    }
}
