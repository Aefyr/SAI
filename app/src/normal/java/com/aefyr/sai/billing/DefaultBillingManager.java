package com.aefyr.sai.billing;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DefaultBillingManager implements BillingManager, PurchasesUpdatedListener, BillingClientStateListener {
    private static final String TAG = "GmsBillingManager";
    private static final String KEY_DONATION_STATUS = "donation_status";
    private static final String SKU_DONATION = "cola";

    private static DefaultBillingManager sInstance;

    private Context mContext;

    private MutableLiveData<BillingManagerStatus> mStatus = new MutableLiveData<>(BillingManagerStatus.NOT_READY);
    private MutableLiveData<DonationStatus> mDonationStatus = new MutableLiveData<>(DonationStatus.UNKNOWN);

    private Set<String> mPurchasedSkus = new HashSet<>();

    private MutableLiveData<List<BillingProduct>> mAllProducts = new MutableLiveData<>(Collections.emptyList());
    private MutableLiveData<List<BillingProduct>> mAvailableProducts = new MutableLiveData<>(Collections.emptyList());

    private BillingClient mBillingClient;

    public static DefaultBillingManager getInstance(Context context) {
        synchronized (DefaultBillingManager.class) {
            return sInstance != null ? sInstance : new DefaultBillingManager(context);
        }
    }

    private DefaultBillingManager(Context context) {
        mContext = context.getApplicationContext();

        if (areGooglePlayServicesAvailable()) {
            mDonationStatus.setValue(getCachedDonationStatus());
            setupGooglePlayBilling();
        } else {
            mStatus.setValue(BillingManagerStatus.NOT_AVAILABLE);
            mDonationStatus.setValue(DonationStatus.NOT_AVAILABLE);
        }

        sInstance = this;
    }

    private void setupGooglePlayBilling() {
        mBillingClient = BillingClient.newBuilder(mContext)
                .enablePendingPurchases()
                .setListener(this)
                .build();

        connectBillingService();
    }

    private void connectBillingService() {
        if (mStatus.getValue() != BillingManagerStatus.NOT_READY)
            return;

        mStatus.setValue(BillingManagerStatus.PREPARING);
        mBillingClient.startConnection(this);
    }

    @Override
    public void onBillingSetupFinished(BillingResult billingResult) {
        if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            //TODO something
            Log.d(TAG, String.format("Unable to setup billing service: %d - %s", billingResult.getResponseCode(), billingResult.getDebugMessage()));
            mStatus.setValue(BillingManagerStatus.NOT_READY);
            return;
        }

        Log.d(TAG, "Billing service connected");

        mStatus.setValue(BillingManagerStatus.OK);
        fetchProductsAndPurchases();
    }

    @Override
    public void onBillingServiceDisconnected() {
        //TODO handle this
        Log.d(TAG, "Billing service disconnected");
        mStatus.setValue(BillingManagerStatus.NOT_READY);
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
    public void launchBillingFlow(Activity activity, BillingProduct product) {
        if (getStatus().getValue() != BillingManagerStatus.OK) {
            //TODO show a dialog
            return;
        }

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(((SkuDetailsBillingProduct) product).getSkuDetails())
                .build();

        mBillingClient.launchBillingFlow(activity, billingFlowParams);
    }

    @Override
    public LiveData<List<BillingProduct>> getAvailableProducts() {
        return mAvailableProducts;
    }

    @Override
    public void refresh() {
        switch (Objects.requireNonNull(getStatus().getValue())) {
            case NOT_READY:
                connectBillingService();
                break;
            case OK:
                fetchProductsAndPurchases();
                break;
            case PREPARING:
            case NOT_AVAILABLE:
                break;
        }
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> list) {
        if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK || list == null) {
            Log.d(TAG, String.format("onPurchasesUpdated called with non-ok code: %d - %s", billingResult.getResponseCode(), billingResult.getDebugMessage()));
            //TODO something
            return;
        }

        for (Purchase purchase : list) {
            if (purchase.getSku().equals(SKU_DONATION)) {
                AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.getPurchaseToken())
                        .build();

                mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, acknowledgePurchaseBillingResult -> {
                    if (acknowledgePurchaseBillingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                        mPurchasedSkus.add(purchase.getSku());
                        setDonationStatus(DonationStatus.DONATED);
                        return;
                    } else {
                        Log.d(TAG, String.format("Unable to acknowledge purchase: %d - %s", acknowledgePurchaseBillingResult.getResponseCode(), acknowledgePurchaseBillingResult.getDebugMessage()));
                        //TODO something
                    }
                });
            }
        }
    }

    private boolean areGooglePlayServicesAvailable() {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mContext) == ConnectionResult.SUCCESS;
    }

    private void fetchProductsAndPurchases() {
        queryPurchases();
        queryProducts();
    }

    private void queryProducts() {
        List<String> skuList = new ArrayList<>();
        skuList.add(SKU_DONATION);

        SkuDetailsParams params = SkuDetailsParams.newBuilder()
                .setSkusList(skuList)
                .setType(BillingClient.SkuType.INAPP)
                .build();

        mBillingClient.querySkuDetailsAsync(params, (billingResult, skuDetailsList) -> {
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                //TODO something
                Log.d(TAG, String.format("Unable to query sku details: %d - %s", billingResult.getResponseCode(), billingResult.getDebugMessage()));
                return;
            }

            ArrayList<BillingProduct> allProducts = new ArrayList<>();
            ArrayList<BillingProduct> availableProducts = new ArrayList<>();

            for (SkuDetails skuDetails : skuDetailsList) {
                BillingProduct billingProduct = new SkuDetailsBillingProduct(skuDetails, mPurchasedSkus.contains(skuDetails.getSku()));
                allProducts.add(billingProduct);

                if (!billingProduct.isPurchased())
                    availableProducts.add(billingProduct);
            }

            mAllProducts.setValue(allProducts);
            mAvailableProducts.setValue(availableProducts);
        });
    }

    private void queryPurchases() {
        Purchase.PurchasesResult purchasesResult = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
        BillingResult billingResult = purchasesResult.getBillingResult();
        if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
            //TODO something
            Log.d(TAG, String.format("Unable to query purchases: %d - %s", billingResult.getResponseCode(), billingResult.getDebugMessage()));
        } else {
            boolean hasDonationPurchase = false;
            for (Purchase purchase : purchasesResult.getPurchasesList()) {
                if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED)
                    continue;

                mPurchasedSkus.add(purchase.getSku());

                if (purchase.getSku().equals(SKU_DONATION))
                    hasDonationPurchase = true;
            }

            setDonationStatus(hasDonationPurchase ? DonationStatus.DONATED : DonationStatus.NOT_DONATED);
        }
    }

    private void setDonationStatus(DonationStatus status) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
        prefs.edit().putString(KEY_DONATION_STATUS, status.name()).apply();
        mDonationStatus.setValue(status);
    }

    private DonationStatus getCachedDonationStatus() {
        try {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mContext);
            return DonationStatus.valueOf(prefs.getString(KEY_DONATION_STATUS, DonationStatus.NOT_DONATED.name()));
        } catch (IllegalArgumentException e) {
            return DonationStatus.NOT_DONATED;
        }
    }
}
