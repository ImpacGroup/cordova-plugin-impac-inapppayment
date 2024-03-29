package de.impacgroup.inapppayment;

import android.app.Activity;
import android.content.Context;
import android.net.ParseException;

import androidx.annotation.NonNull;
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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class IMPBillingManager implements PurchasesUpdatedListener, AcknowledgePurchaseResponseListener {

    private BillingClient billingClient;
    private IMPBillingClientState state;
    private SkuDetailsParams.Builder skuParamsBuilder;
    private IMPBillingManagerListener listener;
    private List<SkuDetails> skuDetails;
    private List<Purchase> mPurchases;
    private IMPSharedPreferencesHelper sharedPreferences;

    private IMPValidationController validationController;
    boolean canMakePurchase = false;

    private Purchase purchaseForAcknowlegde;

    IMPBillingManager(Context context) {

        billingClient = BillingClient.newBuilder(context).setListener(this).enablePendingPurchases().build();
        sharedPreferences = new IMPSharedPreferencesHelper(context);
        validationController = new IMPValidationController(context);
    }

    void createConnection() {
        state = IMPBillingClientState.connecting;
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                state = IMPBillingClientState.connected;
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    canMakePurchase = true;
                    loadPurchases();
                    if (validationController.configIsSet()) {
                        performOpenValidation();
                    }
                    // The BillingClient is ready. You can query purchases here.
                    refreshStatus();
                } else if (listener != null) {
                    canMakePurchase = false;
                    listener.failedStore(IMPBillingResultHelper.getDescriptionFor(billingResult.getResponseCode()));
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
        Purchase.PurchasesResult result = billingClient.queryPurchases(BillingClient.SkuType.SUBS);
        mPurchases = result.getPurchasesList();
    }

    private @Nullable String getTokenFor(String sku) {
        if (mPurchases != null) {
            for (Purchase purchase : mPurchases) {
                if (purchase.isAcknowledged()) {
                    for (String psku : purchase.getSkus()) {
                        if (psku.equals(sku)) {
                            return purchase.getPurchaseToken();
                        }
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
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK
                && list != null) {
            for (Purchase purchase : list) {
                handlePurchase(purchase);
            }
        } else if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.USER_CANCELED) {
            if (list != null) {
                for (Purchase purchase : list) {
                    for (String psku : purchase.getSkus()) {
                        listener.finishedPurchase(psku);
                    }
                }
            } else {
                listener.finishedPurchase(null);
            }
        } else {
            listener.failedPurchase(IMPBillingResultHelper.getDescriptionFor(billingResult.getResponseCode()));
        }
    }

    public void refreshStatus() {
        Date lastDate = sharedPreferences.getRefreshDate();
        if (lastDate != null) {
            Calendar calLast = Calendar.getInstance();
            Calendar calCurrent = Calendar.getInstance();
            try {
                calLast.setTime(lastDate);
                calLast.add(Calendar.DAY_OF_MONTH, 1);
                if (calLast.before(calCurrent)) {
                    refresh();
                }
            } catch(ParseException e) {
                e.printStackTrace();
                refresh();
            }
        } else {
            refresh();
        }
    }

    private void refresh() {
        Purchase.PurchasesResult purchasesResult = billingClient.queryPurchases(BillingClient.SkuType.SUBS);
        List<Purchase> purchases = purchasesResult.getPurchasesList();
        if (purchases != null) {
            for (Purchase purchase : purchases) {

                performValidation(purchase);
            }
            sharedPreferences.storeRefreshDate(new Date());
        }
    }

    /**
     * Sets the information to perform validation against server.
     * @param accessToken Token to identify at the server application
     * @param url url to the rest api
     * @param type tpye of the access token (Bearer…)
     */
    public void setValidation(String accessToken, String url, String type) {
        validationController.setConfig(new IMPValidationConfig(url, accessToken, type));
    }

    /**
     * Performs validation for purchases that are not yet validated. Open validation can happen if network connection was disconnected during validation.
     */
    private void performOpenValidation() {
        Set<String> tokens = sharedPreferences.getTokensForValidation();
        if (tokens != null && mPurchases != null) {
            for (String token: tokens) {
                Purchase purchase = findPurchaseFor(token);
                if (purchase != null) {
                    performValidation(purchase);
                } else {
                    removeIfStored(token);
                }
            }
        }
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
                performValidation(purchase);
            }
        } else if (purchase.getPurchaseState() == PurchaseState.PENDING) {
            for (String psku : purchase.getSkus()) {
                listener.pendingPurchase(psku);
            }
        }
    }

    /**
     * Loads sku details
     */
    void getProducts() {
        billingClient.querySkuDetailsAsync(skuParamsBuilder.build(), new SkuDetailsResponseListener() {
            @Override
            public void onSkuDetailsResponse(@NonNull BillingResult billingResult, List<SkuDetails> list) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
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
            BillingFlowParams.Builder billingFlowParams = BillingFlowParams.newBuilder();
            BillingFlowParams.SubscriptionUpdateParams.Builder builder = BillingFlowParams.SubscriptionUpdateParams.newBuilder();
            billingFlowParams.setSkuDetails(skuDetail);

            if (oldSkuDetail != null) {
                String oldToken = getTokenFor(oldSkuDetail.getSku());

                if (oldToken != null) {
                    builder.setOldSkuPurchaseToken(oldToken);
                    builder.setReplaceSkusProrationMode(BillingFlowParams.ProrationMode.IMMEDIATE_WITH_TIME_PRORATION);

                    BillingFlowParams.SubscriptionUpdateParams updateParams = builder.build();
                    billingFlowParams.setSubscriptionUpdateParams(updateParams);

                }
            }

            BillingFlowParams flowParams = billingFlowParams.build();
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
    public void onAcknowledgePurchaseResponse(@NonNull BillingResult billingResult) {
        performValidation(this.purchaseForAcknowlegde);
    }

    /**
     * Performs a validation for a purchase.
     * @param purchase Purchase to be validated
     */
    private void performValidation(final Purchase purchase) {
        for (String psku : purchase.getSkus()) {
            IMPValidationModel model = new IMPValidationModel(purchase.getPurchaseToken(), psku);
            validationController.validate(model, new IMPValidationController.IMPValidationListener() {

                @Override
                public void failedValidation(String error) {
                    storeOpenValidation(purchase.getPurchaseToken());
                    listener.failedPurchase(error);
                }

                @Override
                public void validationFinished(boolean isValid) {
                    if (listener != null) {
                        listener.finishedPurchase(psku);
                    }
                    removeIfStored(purchase.getPurchaseToken());
                }
            });
        }
    }

    /**
     * Stores a Purchasetoken to validate it later. Token get stored in SharedPreferences.
     * @param token Purchase token to store.
     */
    private void storeOpenValidation(String token) {
        Set<String> tokens = sharedPreferences.getTokensForValidation();
        if (this.find(token, tokens) == null) {
            tokens.add(token);
            sharedPreferences.storeTokenForValidation(tokens);
        }
    }

    private void removeIfStored(String token) {
        Set<String> tokens = sharedPreferences.getTokensForValidation();
        String mToken = this.find(token, tokens);
        if (mToken != null) {
            tokens.remove(mToken);
            sharedPreferences.storeTokenForValidation(tokens);
        }
    }

    /**
     * Search for a token in a set of tokens.
     * @param token String to search for.
     * @param tokens Set of Strings.
     * @return Token if found.
     */
    private @Nullable String find(String token, Set<String> tokens) {
        for (String mToken: tokens) {
            if (mToken.equals(token)) {
                return mToken;
            }
        }
        return null;
    }

    private @Nullable Purchase findPurchaseFor(String token) {
        for (Purchase mPurchase: this.mPurchases) {
            if (mPurchase.getPurchaseToken().equals(token)) {
                return mPurchase;
            }
        }
        return null;
    }

    /**
     * Closes the connection to billing client.
     */
    public void endBilling() {
        state = IMPBillingClientState.closed;
        billingClient.endConnection();
    }
}
