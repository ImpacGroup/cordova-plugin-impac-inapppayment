package de.impacgroup.inapppayment;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.Purchase.PurchaseState;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.SkuDetails;
import com.android.billingclient.api.SkuDetailsParams;
import com.android.billingclient.api.SkuDetailsResponseListener;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.cordova.Whitelist.TAG;

public class IMPBillingManager implements PurchasesUpdatedListener, AcknowledgePurchaseResponseListener {

    private BillingClient billingClient;
    private IMPBillingClientState state;
    private SkuDetailsParams.Builder skuParamsBuilder;
    private IMPBillingManagerListener listener;
    private List<SkuDetails> skuDetails;
    private List<Purchase> mPurchases;

    private Purchase purchaseForAcknowlegde;

    // http config stuff
    private IMPValidationConfig config;
    private RequestQueue queue;

    IMPBillingManager(Context context) {
        billingClient = BillingClient.newBuilder(context).setListener(this).enablePendingPurchases().build();
        queue = Volley.newRequestQueue(context);
        createConnection();
    }

    private void createConnection() {
        state = IMPBillingClientState.connecting;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(BillingResult billingResult) {
                state = IMPBillingClientState.connected;
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    loadPurchases();
                    // The BillingClient is ready. You can query purchases here.
                    refreshStatus();
                } else {
                    recreateConnection();
                }
            }
            @Override
            public void onBillingServiceDisconnected() {
                recreateConnection();
            }
        });
    }

    private void recreateConnection() {
        if (state != IMPBillingClientState.closed) {
            state = IMPBillingClientState.disconnected;
            createConnection();
        }
    }

    private void loadPurchases() {
        Purchase.PurchasesResult result = billingClient.queryPurchases(BillingClient.SkuType.INAPP);
        mPurchases = result.getPurchasesList();
    }

    private @Nullable String getTokenFor(String sku) {
        if (mPurchases != null) {
            for (Purchase purchase : mPurchases) {
                if (purchase.isAcknowledged()) {
                    if (purchase.getSku().equals(sku)) {
                        return purchase.getPurchaseToken();
                    }
                }
            }
        }
        return null;
    }

    public void setListener(IMPBillingManagerListener listener) {
        this.listener = listener;
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
            for (Purchase purchase : list) {
                listener.finishedPurchase(purchase.getSku());
            }
        } else {
            listener.failedPurchase(IMPBillingResultHelper.getDescriptionFor(billingResult.getResponseCode()));
        }
    }

    public void refreshStatus() {
        Log.d(TAG, "IMPAC Loading purchases");
        Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.SUBS);
        List<Purchase> purchases = purchasesResult.getPurchasesList();
        if (purchases != null) {
            for (Purchase purchase : purchases) {
                Log.d(TAG, "purchases: " + purchase.getSku() + " " + purchase.getOrderId() + " " + purchase.getPurchaseToken());
                sendPurchaseToAPI(purchase);
            }
        }
    }

    public void setValidation(String accessToken, String url, String type) {
        this.config = new IMPValidationConfig(url, accessToken, type);
    }

    private void handlePurchase(Purchase purchase) {
        if (purchase.getPurchaseState() == PurchaseState.PURCHASED) {
            // Acknowledge the purchase if it hasn't already been acknowledged.
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams =
                        AcknowledgePurchaseParams.newBuilder()
                                .setPurchaseToken(purchase.getPurchaseToken())
                                .build();
                this.purchaseForAcknowlegde = purchase;
                billingClient.acknowledgePurchase(acknowledgePurchaseParams, this);
            } else {
                sendPurchaseToAPI(purchase);
            }
        } else if (purchase.getPurchaseState() == PurchaseState.PENDING) {
            listener.pendingPurchase(purchase.getSku());
        }
    }

    /**
     * Loads sku details
     */
    void getProducts() {
        billingClient.querySkuDetailsAsync(skuParamsBuilder.build(), new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(BillingResult billingResult, List<SkuDetails> list) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d(TAG, "onSkuDetailsResponse: " + list);
                    skuDetails = list;
                    List<IMPProduct> products = new ArrayList<>();
                    for (SkuDetails skuDetail: list) {
                        products.add(new IMPProduct(skuDetail));
                    }
                    listener.productsLoaded(products);
                } else {
                    String statusString = IMPBillingResultHelper.getDescriptionFor(billingResult.getResponseCode());
                    listener.failedLoadingProducts(statusString);
                }
            }
        });

    }

    void setIDs(List<String> ids) {
        skuParamsBuilder = SkuDetailsParams.newBuilder();
        skuParamsBuilder.setSkusList(ids).setType(BillingClient.SkuType.SUBS);
    }

    void buyProduct(String id, Activity activity, String oldSku) {
        SkuDetails skuDetail = getSkuDetailsBy(id);
        SkuDetails oldSkuDetail = null;
        if (oldSku != null) {
            oldSkuDetail = getSkuDetailsBy(oldSku);
        }

        if (skuDetail != null) {
            BillingFlowParams.Builder builder = BillingFlowParams.newBuilder();
            builder.setSkuDetails(skuDetail);
            if (oldSkuDetail != null) {
                builder.setOldSku(oldSkuDetail.getSku(), getTokenFor(oldSkuDetail.getSku()));
                builder.setReplaceSkusProrationMode(BillingFlowParams.ProrationMode.IMMEDIATE_WITH_TIME_PRORATION);
                Log.d(TAG, "buyProduct: crossgrade from " + oldSkuDetail.getSku());
            } else {
                Log.d(TAG, "buyProduct: no crossgrade");
            }

            BillingFlowParams flowParams = builder.build();
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

    @Override
    public void onAcknowledgePurchaseResponse(BillingResult billingResult) {

        this.sendPurchaseToAPI(this.purchaseForAcknowlegde);
    }

    private void sendPurchaseToAPI(final Purchase purchase) {

        if (purchase != null && this.config != null) {
            Map<String, String> data = new HashMap<>();
            data.put("purchaseToken", purchase.getPurchaseToken());
            data.put("productId", purchase.getSku());

            JsonObjectRequest postRequest = new JsonObjectRequest(Request.Method.POST, this.config.url, new JSONObject(data),
                    new Response.Listener<JSONObject>()
                    {
                        @Override
                        public void onResponse(JSONObject response) {
                            // response
                            if (listener != null) {
                                listener.finishedPurchase(purchase.getSku());
                            }
                        }
                    },
                    new Response.ErrorListener()
                    {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            // error
                            if (listener != null) {
                                listener.failedPurchase(error.toString());
                            }
                        }
                    }
            ) {

                /**
                 * Passing auth request headers
                 */
                @Override
                public Map<String, String> getHeaders() {
                    HashMap<String, String> headers = new HashMap<>();
                    headers.put("Content-Type", "application/json");
                    headers.put("Authorization", config.authorizationType + " " + config.accessString);
                    return headers;
                }
            };
            queue.add(postRequest);
        } else if (listener != null) {
            listener.failedPurchase("Unknown error");
        }
    }

    /**
     * Closes the connection to billing client.
     */
    public void endBilling() {
        state = IMPBillingClientState.closed;
        billingClient.endConnection();
    }
}
