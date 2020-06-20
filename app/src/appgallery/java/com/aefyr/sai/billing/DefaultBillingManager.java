package com.aefyr.sai.billing;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aefyr.sai.R;

import java.util.Collections;
import java.util.List;

public class DefaultBillingManager implements BillingManager {

    private static DefaultBillingManager sInstance;

    private MutableLiveData<BillingManagerStatus> mStatus = new MutableLiveData<>(BillingManagerStatus.OK);
    private MutableLiveData<DonationStatus> mDonationStatus = new MutableLiveData<>(DonationStatus.DONATED);

    private MutableLiveData<List<BillingProduct>> mProducts = new MutableLiveData<>(Collections.emptyList());
    private MutableLiveData<List<BillingProduct>> mPurchasedProducts = new MutableLiveData<>(Collections.emptyList());

    public static DefaultBillingManager getInstance(Context context) {
        synchronized (DefaultBillingManager.class) {
            return sInstance != null ? sInstance : new DefaultBillingManager(context);
        }
    }

    private DefaultBillingManager(Context context) {
        sInstance = this;
    }

    @Override
    public LiveData<BillingManagerStatus> getStatus() {
        return mStatus;
    }

    @Override
    public LiveData<DonationStatus> getDonationStatus() {
        return mDonationStatus;
    }

    @Override
    public DonationStatusRenderer getDonationStatusRenderer() {
        return new FDroidDonationStatusRenderer();
    }

    @Override
    public LiveData<List<BillingProduct>> getAllProducts() {
        return mProducts;
    }

    @Override
    public LiveData<List<BillingProduct>> getPurchasedProducts() {
        return mPurchasedProducts;
    }

    @Override
    public LiveData<List<BillingProduct>> getPurchasableProducts() {
        return mProducts;
    }

    @Override
    public void launchBillingFlow(Activity activity, BillingProduct product) {

    }

    @Override
    public void refresh() {

    }

    private static class FDroidDonationStatusRenderer implements DonationStatusRenderer {

        @Override
        public String getText(Context context, DonationStatus donationStatus) {
            return "placeholder";
        }

        @Override
        public Drawable getIcon(Context context, DonationStatus donationStatus) {
            return context.getResources().getDrawable(R.drawable.ic_donation_status_floss);
        }
    }
}
