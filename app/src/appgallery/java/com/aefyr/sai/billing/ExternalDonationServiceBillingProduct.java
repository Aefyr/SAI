package com.aefyr.sai.billing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ExternalDonationServiceBillingProduct implements BillingProduct {

    private String mId;
    private String mTitle;
    private String mDesc;
    private String mIconUrl;
    private String mTargetUrl;

    public ExternalDonationServiceBillingProduct(String id, String title, String description, String iconUri, String targetUrl) {
        mId = id;
        mTitle = title;
        mDesc = description;
        mIconUrl = iconUri;
        mTargetUrl = targetUrl;
    }

    @NonNull
    @Override
    public String getTitle() {
        return mTitle;
    }

    @Nullable
    @Override
    public String getDescription() {
        return mDesc;
    }

    @Nullable
    @Override
    public String getIconUrl() {
        return mIconUrl;
    }

    @NonNull
    @Override
    public String getPrice() {
        return "";
    }

    @NonNull
    @Override
    public String getId() {
        return mId;
    }

    @Override
    public boolean isPurchased() {
        return false;
    }

    public String getTargetUrl() {
        return mTargetUrl;
    }
}
