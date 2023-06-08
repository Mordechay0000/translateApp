package com.mordechay.myd.translateapp;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.google.firebase.crashlytics.internal.model.ImmutableList;

import java.util.List;

public class BillingPremium implements PurchasesUpdatedListener, PurchasesResponseListener, BillingClientStateListener, ProductDetailsResponseListener {
    private Activity act;
    private final IsPremiumListener isPremiumListener;
    private final QueryProductDetailsParams queryProductDetailsParams;

    public void setActivity(Activity act) {
        this.act = act;
    }

    private final BillingClient billingClient;

    public BillingPremium(@NonNull Activity act, IsPremiumListener isPremiumListener) {
        this.act = act;
        this.isPremiumListener = isPremiumListener;
        isPremiumListener.premiumUpdateListener(true);
        billingClient = BillingClient.newBuilder(act)
                .setListener(this)
                .enablePendingPurchases()
                .build();
        queryProductDetailsParams =
                QueryProductDetailsParams.newBuilder()
                        .setProductList(
                                ImmutableList.from(
                                        QueryProductDetailsParams.Product.newBuilder()
                                                .setProductId("translate_premium")
                                                .setProductType(BillingClient.ProductType.SUBS)
                                                .build()))
                        .build();
    }


    public void checkingPremium() {
        isPremiumListener.premiumUpdateListener(true);
        /*
        if (billingClient.getConnectionState() == BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
            billingClient.startConnection(this);
        else if (billingClient.getConnectionState() == BillingClient.BillingResponseCode.OK)
            billingClient.queryProductDetailsAsync(
                    queryProductDetailsParams, this);
         */
    }

    @Override
    public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> list) {

    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult, @Nullable List<Purchase> list) {

    }

    @Override
    public void onBillingServiceDisconnected() {

    }

    @Override
    public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
        int responseCode = billingResult.getResponseCode();
        if (responseCode == BillingClient.BillingResponseCode.OK) {
            billingClient.queryProductDetailsAsync(
                    queryProductDetailsParams, this);
        } else {
            isPremiumListener.premiumUpdateListener(false);
        }
    }

    @Override
    public void onProductDetailsResponse(@NonNull BillingResult billingResult, @NonNull List<ProductDetails> list) {
        assert list.get(0).getSubscriptionOfferDetails() != null;
        ImmutableList<BillingFlowParams.ProductDetailsParams> productDetailsParamsList =
                ImmutableList.from(
                        BillingFlowParams.ProductDetailsParams.newBuilder()
                                // retrieve a value for "productDetails" by calling queryProductDetailsAsync()
                                .setProductDetails(list.get(0))
                                // to get an offer token, call ProductDetails.getSubscriptionOfferDetails()
                                // for a list of offers that are available to the user
                                .setOfferToken(list.get(0).getSubscriptionOfferDetails().get(0).getOfferToken())
                                .build()
                );

        BillingFlowParams billingFlowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(productDetailsParamsList)
                .build();

// Launch the billing flow
        BillingResult billingResultt = billingClient.launchBillingFlow(act, billingFlowParams);

    }
}
