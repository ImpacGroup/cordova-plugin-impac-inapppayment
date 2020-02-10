package de.impacgroup.inapppayment;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.List;

import static org.apache.cordova.Whitelist.TAG;

public class IMPBillingManager implements PurchasesUpdatedListener {

    private BillingClient billingClient;
    private SkuDetailsParams.Builder skuParamsBuilder;

    IMPBillingManager(Context context) {
        billingClient = BillingClient.newBuilder(context).setListener(this).enablePendingPurchases().build();

        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "onBillingSetupFinished:  OK");
                    // The BillingClient is ready. You can query purchases here.
                } else {
                    Log.d(TAG, "onBillingSetupFinished: " + billingResult.getResponseCode());
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                Log.d(TAG, "onBillingServiceDisconnected: ");
                // Try to restart the connection on the next request to
                // Google Play by calling the startConnection() method.
            }
        });
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> list) {
        Log.d(TAG, "onPurchasesUpdated: " + billingResult);
    }

    void getProducts() {
        Log.d(TAG, "getProducts: " + "");
        billingClient.querySkuDetailsAsync(skuParamsBuilder.build(), new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> list) {
                Log.d(TAG, "onSkuDetailsResponse: " + list);
            }
        });

    }

    void setIDs(List<String> ids) {
        skuParamsBuilder = SkuDetailsParams.newBuilder();
        skuParamsBuilder.setSkusList(ids).setType(BillingClient.SkuType.INAPP);
    }
}
