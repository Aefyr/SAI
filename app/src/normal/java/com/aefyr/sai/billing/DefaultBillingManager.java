package com.aefyr.sai.billing;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.preference.PreferenceManager;

import com.aefyr.sai.R;
import com.aefyr.sai.ui.dialogs.SimpleAlertDialogFragment;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

//TODO rewrite this
public class DefaultBillingManager implements BillingManager, PurchasesUpdatedListener, BillingClientStateListener {
    private static final String TAG = "GmsBillingManager";
    private static final String KEY_DONATION_STATUS = "donation_status";
    private static final String SKU_DONATION = "cola";

    private static DefaultBillingManager sInstance;

    private MutableLiveData<BillingManagerStatus> mBillingStatus = new MutableLiveData<>(BillingManagerStatus.NOT_READY);
    private MutableLiveData<DonationStatus> mDonationStatus = new MutableLiveData<>(DonationStatus.UNKNOWN);

    private Set<String> mPurchasedSkus = new HashSet<>();

    private MutableLiveData<List<BillingProduct>> mAllProducts = new MutableLiveData<>(Collections.emptyList());
    private MutableLiveData<List<BillingProduct>> mPurchasedProducts = new MutableLiveData<>(Collections.emptyList());
    private MutableLiveData<List<BillingProduct>> mPurchasableProducts = new MutableLiveData<>(Collections.emptyList());

    private Context mContext;

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
            mBillingStatus.setValue(BillingManagerStatus.NOT_AVAILABLE);
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
        if (mBillingStatus.getValue() != BillingManagerStatus.NOT_READY)
            return;

        mBillingStatus.setValue(BillingManagerStatus.PREPARING);
        mBillingClient.startConnection(this);
    }

    @Override
    public LiveData<BillingManagerStatus> getStatus() {
        return mBillingStatus;
    }

    @Override
    public LiveData<DonationStatus> getDonationStatus() {
        return mDonationStatus;
    }

    @Override
    public LiveData<List<BillingProduct>> getAllProducts() {
        return mAllProducts;
    }

    @Override
    public LiveData<List<BillingProduct>> getPurchasedProducts() {
        return mPurchasedProducts;
    }

    @Override
    public LiveData<List<BillingProduct>> getPurchasableProducts() {
        return mPurchasableProducts;
    }

    @Override
    public void launchBillingFlow(Activity activity, BillingProduct product) {
        if (getStatus().getValue() != BillingManagerStatus.OK) {
            if (activity instanceof FragmentActivity) {
                FragmentActivity fragmentActivity = (FragmentActivity) activity;
                SimpleAlertDialogFragment.newInstance(activity, R.string.error, R.string.donate_billing_not_available).show(fragmentActivity.getSupportFragmentManager(), null);
            }
            return;
        }

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setSkuDetails(((SkuDetailsBillingProduct) product).getSkuDetails())
                .build();

        mBillingClient.launchBillingFlow(activity, billingFlowParams);
    }

    @Override
    public void refresh() {
        switch (Objects.requireNonNull(getStatus().getValue())) {
            case NOT_READY:
                connectBillingService();
                break;
            case OK:
                loadPurchases();
                loadProducts();
                break;
            case PREPARING:
            case NOT_AVAILABLE:
                break;
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

    private boolean areGooglePlayServicesAvailable() {
        return GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(mContext) == ConnectionResult.SUCCESS;
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> list) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED) {
            connectBillingService();
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            loadPurchases();
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            loadPurchases();
        }
    }

    private void loadPurchases() {
        Purchase.PurchasesResult result = mBillingClient.queryPurchases(BillingClient.SkuType.INAPP);
        if (result.getBillingResult().getResponseCode() != BillingClient.BillingResponseCode.OK) {
            Log.w(TAG, String.format("Unable to load purchases, code %d - %s", result.getBillingResult().getResponseCode(), result.getBillingResult().getDebugMessage()));
            return;
        }

        processPurchases(result.getPurchasesList());
    }

    private void processPurchases(Collection<Purchase> purchases) {
        if (purchases == null)
            return;

        mPurchasedSkus.clear();
        boolean containsDonationPurchase = false;
        for (Purchase purchase : purchases) {
            if (purchase.getPurchaseState() == Purchase.PurchaseState.UNSPECIFIED_STATE) {
                Log.w(TAG, String.format("Purchase in unspecified state - %s", purchase));
                continue;
            }

            if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED && !purchase.isAcknowledged()) {
                acknowledgePurchase(purchase);
            } else {
                if (purchase.getSku().equals(SKU_DONATION)) {
                    containsDonationPurchase = true;
                    if (purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                        setDonationStatus(DonationStatus.DONATED);
                    } else if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
                        setDonationStatus(DonationStatus.PENDING);
                    }
                }

                mPurchasedSkus.add(purchase.getSku());
            }
        }

        if (!containsDonationPurchase)
            setDonationStatus(DonationStatus.NOT_DONATED);

        invalidateProductsPurchaseStatus();
    }

    private void acknowledgePurchase(Purchase purchase) {
        AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();

        mBillingClient.acknowledgePurchase(acknowledgePurchaseParams, billingResult -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, String.format("Acknowledged %s", purchase.getSku()));
                loadPurchases();
                return;
            }

            Log.w(TAG, String.format("Unable to acknowledge purchase, code %d - %s", billingResult.getResponseCode(), billingResult.getDebugMessage()));
        });
    }

    private void loadProducts() {
        List<String> skuList = new ArrayList<>();
        skuList.add(SKU_DONATION);

        SkuDetailsParams params = SkuDetailsParams.newBuilder()
                .setSkusList(skuList)
                .setType(BillingClient.SkuType.INAPP)
                .build();

        mBillingClient.querySkuDetailsAsync(params, (billingResult, skuDetailsList) -> {
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, String.format("Unable to query sku details: %d - %s", billingResult.getResponseCode(), billingResult.getDebugMessage()));
                return;
            }

            ArrayList<BillingProduct> products = new ArrayList<>(skuDetailsList.size());
            for (SkuDetails skuDetails : skuDetailsList)
                products.add(new SkuDetailsBillingProduct(skuDetails));

            mAllProducts.setValue(products);
            invalidateProductsPurchaseStatus();
        });
    }

    private void invalidateProductsPurchaseStatus() {
        List<BillingProduct> allProducts = mAllProducts.getValue();
        if (allProducts == null)
            return;

        List<BillingProduct> purchasedProducts = new ArrayList<>();
        List<BillingProduct> purchasableProducts = new ArrayList<>();
        for (BillingProduct product : allProducts) {
            SkuDetailsBillingProduct skuProduct = (SkuDetailsBillingProduct) product;

            boolean purchased = mPurchasedSkus.contains(skuProduct.getSkuDetails().getSku());
            skuProduct.setPurchased(purchased);

            if (mPurchasedSkus.contains(product.getId()))
                purchasedProducts.add(product);
            else
                purchasableProducts.add(product);
        }

        mPurchasedProducts.setValue(purchasedProducts);
        mPurchasableProducts.setValue(purchasableProducts);

        Log.d(TAG, "Invalidated products purchase status");
    }

    @Override
    public void onBillingSetupFinished(BillingResult billingResult) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
            mBillingStatus.setValue(BillingManagerStatus.OK);
            Log.d(TAG, "Billing service connected");
            loadPurchases();
            loadProducts();
        } else {
            mBillingStatus.setValue(BillingManagerStatus.NOT_AVAILABLE);
            Log.w(TAG, String.format("Unable to connect to billing service, code %d - %s", billingResult.getResponseCode(), billingResult.getDebugMessage()));
        }
    }

    @Override
    public void onBillingServiceDisconnected() {
        Log.d(TAG, "Billing service disconnected, reconnecting");
        mBillingStatus.setValue(BillingManagerStatus.NOT_READY);
        connectBillingService();
    }
}
