package com.aefyr.sai.billing;

import android.app.Activity;

import androidx.lifecycle.LiveData;

import java.util.List;

public interface BillingManager {

    LiveData<BillingManagerStatus> getStatus();

    LiveData<DonationStatus> getDonationStatus();

    LiveData<List<BillingProduct>> getAvailableProducts();

    void launchBillingFlow(Activity activity, BillingProduct product);

    void refresh();

}
