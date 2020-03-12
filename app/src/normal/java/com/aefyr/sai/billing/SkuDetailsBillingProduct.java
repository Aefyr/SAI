package com.aefyr.sai.billing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.SkuDetails;

public class SkuDetailsBillingProduct implements BillingProduct {

    private SkuDetails mSkuDetails;
    private boolean mIsPurchased;

    public SkuDetailsBillingProduct(SkuDetails skuDetails) {
        mSkuDetails = skuDetails;
    }

    @NonNull
    @Override
    public String getTitle() {
        return mSkuDetails.getTitle();
    }

    @Nullable
    @Override
    public String getDescription() {
        return mSkuDetails.getDescription();
    }

    @Nullable
    @Override
    public String getIconUrl() {
        return mSkuDetails.getIconUrl();
    }

    @NonNull
    @Override
    public String getPrice() {
        return mSkuDetails.getPrice();
    }

    @NonNull
    @Override
    public String getId() {
        return mSkuDetails.getSku();
    }

    void setPurchased(boolean purchased) {
        mIsPurchased = purchased;
    }

    @Override
    public boolean isPurchased() {
        return mIsPurchased;
    }

    public SkuDetails getSkuDetails() {
        return mSkuDetails;
    }
}
