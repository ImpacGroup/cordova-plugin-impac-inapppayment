package de.impacgroup.inapppayment;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;

import java.util.ArrayList;
import java.util.List;

import static org.apache.cordova.Whitelist.TAG;

public class IMPBillingManager implements PurchasesUpdatedListener {

    private BillingClient billingClient;
    private SkuDetailsParams.Builder skuParamsBuilder;
    private List<SkuDetails> skuDetails;

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
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && list != null) {
            for (Purchase purchase : list) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            // Handle an error caused by a user cancelling the purchase flow.
            Log.d(TAG, "onPurchasesUpdated: " + "User Canceled");
        } else {
            Log.d(TAG, "onPurchasesUpdated: " + billingResult.getResponseCode());
            // Handle any other error codes.
        }
    }

    private void handlePurchase(Purchase purchase) {
        Log.d(TAG, "handlePurchase: " + purchase.getPurchaseToken());
    }

    void getProducts(final IMPBillingManagerProductListener listener) {
        Log.d(TAG, "getProducts: ");
        billingClient.querySkuDetailsAsync(skuParamsBuilder.build(), new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> list) {
                Log.d(TAG, "onSkuDetailsResponse: " + list);
                skuDetails = list;
                List<IMPProduct> products = new ArrayList<>();
                for (SkuDetails skuDetail: list) {
                    products.add(new IMPProduct(skuDetail));
                }
                listener.productsLoaded(products);
            }
        });

    }

    void setIDs(List<String> ids) {
        Log.d(TAG, "setIDs: " + ids);
        skuParamsBuilder = SkuDetailsParams.newBuilder();
        skuParamsBuilder.setSkusList(ids).setType(BillingClient.SkuType.INAPP);
    }

    void buyProduct(String id, Activity activity) {
        SkuDetails skuDetail = getSkuDetailsBy(id);
        if (skuDetail != null) {
            BillingFlowParams flowParams = BillingFlowParams.newBuilder().setSkuDetails(skuDetail).build();
            billingClient.launchBillingFlow(activity, flowParams);
        }
    }

    private @Nullable SkuDetails getSkuDetailsBy(String id) {
        for ( SkuDetails skudetail : skuDetails) {
            if (skudetail.getSku().equals(id)) {
                return skudetail;
            }
        }
        return null;
    }
}
