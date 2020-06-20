package com.aefyr.sai.billing;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.aefyr.sai.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DefaultBillingManager implements BillingManager {

    private static DefaultBillingManager sInstance;

    private MutableLiveData<BillingManagerStatus> mStatus = new MutableLiveData<>(BillingManagerStatus.OK);
    private MutableLiveData<DonationStatus> mDonationStatus = new MutableLiveData<>(DonationStatus.DONATED);

    private MutableLiveData<List<BillingProduct>> mProducts = new MutableLiveData<>();
    private MutableLiveData<List<BillingProduct>> mPurchasedProducts = new MutableLiveData<>(Collections.emptyList());

    public static DefaultBillingManager getInstance(Context context) {
        synchronized (DefaultBillingManager.class) {
            return sInstance != null ? sInstance : new DefaultBillingManager(context);
        }
    }

    private DefaultBillingManager(Context context) {
        createProducts(context.getApplicationContext());

        sInstance = this;
    }

    private void createProducts(Context c) {
        List<BillingProduct> products = new ArrayList<>();
        products.add(new ExternalDonationServiceBillingProduct("yandex", c.getString(R.string.donate_non_iap_yandex_title), c.getString(R.string.donate_non_iap_yandex_desc), null, c.getString(R.string.donate_non_iap_yandex_target)));
        mProducts.setValue(products);
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
        ExternalDonationServiceBillingProduct extProduct = (ExternalDonationServiceBillingProduct) product;
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(extProduct.getTargetUrl()));
            activity.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            //whatever
        }

    }

    @Override
    public void refresh() {

    }

    private static class FDroidDonationStatusRenderer implements DonationStatusRenderer {

        @Override
        public String getText(Context context, DonationStatus donationStatus) {
            return context.getString(R.string.donate_message_floss);
        }

        @Override
        public Drawable getIcon(Context context, DonationStatus donationStatus) {
            return context.getResources().getDrawable(R.drawable.ic_donation_status_floss);
        }
    }
}
