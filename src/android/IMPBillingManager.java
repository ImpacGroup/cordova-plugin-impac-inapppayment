package de.impacgroup.inapppayment;

import android.app.Activity;
import android.content.Context;
import android.net.ParseException;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.AcknowledgePurchaseResponseListener;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.ProductDetailsResponseListener;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.Purchase.PurchaseState;
import com.android.billingclient.api.PurchasesResponseListener;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;

public class IMPBillingManager implements PurchasesUpdatedListener, AcknowledgePurchaseResponseListener {

    private final BillingClient billingClient;
    private IMPBillingClientState state;
    private QueryProductDetailsParams.Builder productParamsBuilder;
    private IMPBillingManagerListener listener;
    private List<ProductDetails> productDetails;
    private List<Purchase> mPurchases;
    private final List<IMPProduct> products = new ArrayList<>(); // Declare and initialize products
    private final IMPSharedPreferencesHelper sharedPreferences;

    private final IMPValidationController validationController;
    boolean canMakePurchase = false;

    private Purchase purchaseForAcknowlegde;

    IMPBillingManager(Context context) {
        billingClient = BillingClient.newBuilder(context).setListener(this).enablePendingPurchases().build();
        sharedPreferences = new IMPSharedPreferencesHelper(context);
        validationController = new IMPValidationController(context);
    }

    void createConnection() {
        state = IMPBillingClientState.connecting;
        Log.d("IMPBillingManager", "Connecting BillingClient");
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    Log.d("IMPBillingManager", "BillingClient setup finished successfully");
                    state = IMPBillingClientState.connected;
                    canMakePurchase = true;
                    loadPurchases();
                } else {
                    Log.e("IMPBillingManager", "BillingClient setup failed with response code: " + billingResult.getResponseCode());
                    state = IMPBillingClientState.closed;
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                Log.e("IMPBillingManager", "BillingClient service disconnected");
                state = IMPBillingClientState.closed;
                canMakePurchase = false;
                recreateConnection();
            }
        });
    }

    private void recreateConnection() {
        if (state != IMPBillingClientState.closed) {
            Log.d("IMPBillingManager", "Recreating BillingClient connection");
            billingClient.endConnection();
            createConnection();
        }
    }

    private void loadPurchases() {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, new PurchasesResponseListener() {
            @Override
            public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> purchases) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    mPurchases = purchases;
                }
            }
        });
    }

    public void setListener(IMPBillingManagerListener listener) {
        this.listener = listener;
    }

    @Override
    public void onPurchasesUpdated(BillingResult billingResult, @Nullable List<Purchase> list) {
        if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK && list != null) {
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
            } catch (ParseException e) {
                e.printStackTrace();
                refresh();
            }
        } else {
            refresh();
        }
    }

    private void refresh() {
        billingClient.queryPurchasesAsync(BillingClient.SkuType.SUBS, new PurchasesResponseListener() {
            @Override
            public void onQueryPurchasesResponse(@NonNull BillingResult billingResult, @NonNull List<Purchase> purchases) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    for (Purchase purchase : purchases) {

                        performValidation(purchase);
                    }
                    sharedPreferences.storeRefreshDate(new Date());
                }
            }
        });
    }

    /**
     * Sets the information to perform validation against server.
     *
     * @param accessToken Token to identify at the server application
     * @param url         url to the rest api
     * @param type        tpye of the access token (Bearer…)
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
            for (String token : tokens) {
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
            if (!purchase.isAcknowledged()) {
                AcknowledgePurchaseParams acknowledgePurchaseParams = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.getPurchaseToken()).build();
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
        billingClient.queryProductDetailsAsync(productParamsBuilder.build(), new ProductDetailsResponseListener() {
            @Override
            public void onProductDetailsResponse(@NonNull BillingResult billingResult, List<ProductDetails> list) {
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    if (list != null) {
                        productDetails = list;
                        List<IMPProduct> products = new ArrayList<>();
                        for (ProductDetails productDetail : list) {
                            try {
                                IMPProduct product = new IMPProduct(productDetail);
                                products.add(product);
                                Log.d("IMPBillingManager", "Added product: " + product);
                            } catch (Exception e) {
                                Log.e("IMPBillingManager", "Error creating IMPProduct: ", e);
                            }
                        }
                        Log.d("IMPBillingManager", "Products are " + products);
                        listener.productsLoaded(products);
                    } else {
                        Log.e("IMPBillingManager", "Product details list is null");
                        listener.failedLoadingProducts("Product details list is null");
                    }
                } else {
                    String statusString = IMPBillingResultHelper.getDescriptionFor(billingResult.getResponseCode());
                    listener.failedLoadingProducts(statusString);
                }
            }
        });
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
        } else {
            Log.e("IMPBillingManager", "mPurchases is null");
        }
        return null;
    }

    private @Nullable Purchase findPurchaseFor(String token) {
        if (mPurchases != null) {
            for (Purchase mPurchase : this.mPurchases) {
                if (mPurchase.getPurchaseToken().equals(token)) {
                    return mPurchase;
                }
            }
        } else {
            Log.e("IMPBillingManager", "mPurchases is null");
        }
        return null;
    }

    void setIDs(List<String> ids) {
        List<QueryProductDetailsParams.Product> productList = new ArrayList<>();
        for (String id : ids) {
            productList.add(QueryProductDetailsParams.Product.newBuilder().setProductId(id).setProductType(BillingClient.ProductType.SUBS).build());
        }
        productParamsBuilder = QueryProductDetailsParams.newBuilder();
        productParamsBuilder.setProductList(productList);
    }

    void buyProduct(String id, Activity activity, String oldSku) {
        if (state != IMPBillingClientState.connected) {
            Log.e("IMPBillingManager", "BillingClient is not connected");
            return;
        }

        ProductDetails productDetail = getProductDetailsBy(id);
        ProductDetails oldProductDetail = null;
        if (oldSku != null) {
            oldProductDetail = getProductDetailsBy(oldSku);
        }

        if (productDetail != null) {
            BillingFlowParams.Builder billingFlowParams = BillingFlowParams.newBuilder();

            // Retrieve the offerToken for the subscription
            String offerToken = null;
            if (productDetail.getSubscriptionOfferDetails() != null && !productDetail.getSubscriptionOfferDetails().isEmpty()) {
                offerToken = productDetail.getSubscriptionOfferDetails().get(0).getOfferToken();
            }

            if (offerToken == null) {
                Log.e("IMPBillingManager", "Offer token is null for product: " + id);
                return;
            }

            BillingFlowParams.ProductDetailsParams productDetailsParams = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(productDetail).setOfferToken(offerToken) // Set the offerToken
                    .build();
            billingFlowParams.setProductDetailsParamsList(Collections.singletonList(productDetailsParams));
            if (oldProductDetail != null) {
                String oldToken = getTokenFor(oldProductDetail.getProductId());
                if (oldToken != null) {
                    BillingFlowParams.SubscriptionUpdateParams.Builder builder = BillingFlowParams.SubscriptionUpdateParams.newBuilder();
                    builder.setOldPurchaseToken(oldToken);
                    billingFlowParams.setSubscriptionUpdateParams(builder.build());
                }
            }

            BillingFlowParams flowParams = billingFlowParams.build();
            billingClient.launchBillingFlow(activity, flowParams);
        } else {
            Log.e("IMPBillingManager", "Product details not found for product: " + id);
        }
    }

    private @Nullable ProductDetails getProductDetailsBy(String id) {
        for (ProductDetails productDetail : productDetails) {
            if (productDetail.getProductId().equals(id)) {
                return productDetail;
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
     *
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
                    listener.validationFinished(isValid);
                }
            });
        }
    }

    /**
     * Stores a Purchasetoken to validate it later. Token get stored in SharedPreferences.
     *
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
     *
     * @param token  String to search for.
     * @param tokens Set of Strings.
     * @return Token if found.
     */
    private @Nullable String find(String token, Set<String> tokens) {
        for (String mToken : tokens) {
            if (mToken.equals(token)) {
                return mToken;
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