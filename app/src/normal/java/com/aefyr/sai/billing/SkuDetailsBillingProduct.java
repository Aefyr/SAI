package com.aefyr.sai.billing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.SkuDetails;

public class SkuDetailsBillingProduct implements BillingProduct {

    private SkuDetails mSkuDetails;

    private boolean mPurchased;

    public SkuDetailsBillingProduct(SkuDetails skuDetails, boolean purchased) {
        mSkuDetails = skuDetails;
        mPurchased = purchased;
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

    @Nullable
    @Override
    public boolean isPurchased() {
        return mPurchased;
    }

    public SkuDetails getSkuDetails() {
        return mSkuDetails;
    }
}
