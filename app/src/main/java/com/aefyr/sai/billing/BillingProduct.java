package com.aefyr.sai.billing;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public interface BillingProduct {

    @NonNull
    String getTitle();

    @Nullable
    String getDescription();

    @Nullable
    String getIconUrl();

    @NonNull
    String getPrice();

    boolean isPurchased();

}
