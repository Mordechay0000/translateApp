package com.mordechay.myd.translateapp;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PurchasesUpdatedListener;

public class BillingPremium {
    private Context cntx;

    public void setCntx(Context cntx) {
        this.cntx = cntx;
    }

    private BillingClient billingClient;
    //billing
    private final PurchasesUpdatedListener purchasesUpdatedListener = (billingResult, purchases) -> {
        // To be implemented in a later section.

    };
    private final BillingClientStateListener billingStateListener = new BillingClientStateListener() {
        @Override
        public void onBillingServiceDisconnected() {
            Toast.makeText(cntx, "שגיאה: לא הצלחנו להתחבר לסיפריית התשלומים, מנסה שוב.", Toast.LENGTH_LONG).show();
            billingClient.startConnection(billingStateListener);
        }

        @Override
        public void onBillingSetupFinished(@NonNull BillingResult billingResult) {

        }
    };

    public BillingPremium(@NonNull Context cntx){
        this.cntx = cntx;

        //billing
        billingClient = BillingClient.newBuilder(cntx)
                .setListener(purchasesUpdatedListener)
                .enablePendingPurchases()
                .build();
        billingClient.startConnection(billingStateListener);
    }

    public void isPremium(){

    }
}
