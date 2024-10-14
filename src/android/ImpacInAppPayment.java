package de.impacgroup.inapppayment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.gson.Gson;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ImpacInAppPayment extends CordovaPlugin {

    private IMPBillingManager billingManager;
    private CallbackContext updateCallbackContext;
    private CallbackContext productCallbackContext;

    // No-argument constructor
    public ImpacInAppPayment() {
    }


    // This init should not be used, instead use the init with cordova and webView params
    public ImpacInAppPayment(Context context) {
        billingManager = new IMPBillingManager(context);
        billingManager.setListener(new IMPBillingManagerListener() {
            @Override
            public void failedPurchase(String error) {
                // Handle failed purchase
            }

            @Override
            public void validationFinished(boolean isValid) {
                // Handle validation finished
            }

            @Override
            public void failedStore(String error) {
                // Handle failed store
            }

            @Override
            public void failedLoadingProducts(String error) {
                if (productCallbackContext != null) {
                    productCallbackContext.error(error);
                } else {
                    // Handle the case where productCallbackContext is null, e.g., log an error

                }
            }

            @Override
            public void productsLoaded(List<IMPProduct> list) {
                // Handle products loaded
            }

            @Override
            public void pendingPurchase(String sku) {
                // Handle pending purchase
            }

            @Override
            public void finishedPurchase(String sku) {
                // Handle finished purchase
            }
        });
    }

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        final Context context = this.cordova.getActivity().getApplicationContext();
        billingManager = new IMPBillingManager(context);
        billingManager.createConnection();
        billingManager.setListener(new IMPBillingManagerListener() {
            @Override
            public void finishedPurchase(@Nullable String sku) {
                sendUpdateMessage(new IMPUpdateMessage("finished", null), PluginResult.Status.OK);
            }

            @Override
            public void pendingPurchase(String sku) {
                sendUpdateMessage(new IMPUpdateMessage("didPause", null), PluginResult.Status.OK);
            }

            @Override
            public void failedPurchase(@Nullable String error) {
                sendUpdateMessage(new IMPUpdateMessage("finished", error), PluginResult.Status.ERROR);
            }

            @Override
            public void productsLoaded(List<IMPProduct> list) {
                if (productCallbackContext != null) {
                    String json = new Gson().toJson(list);
                    productCallbackContext.success(json);
                } else {
                    Log.e("IMPBillingManager", "productCallbackContext is null");
                }
            }

            @Override
            public void validationFinished(boolean isValid) {
                Log.d("IMPBillingManager", "Validation finished: " + isValid);
            }

            @Override
            public void failedStore(String error) {
                sendUpdateMessage(new IMPUpdateMessage("connection", error), PluginResult.Status.ERROR);
            }

            @Override
            public void failedLoadingProducts(String error) {
                if (productCallbackContext != null) {
                    productCallbackContext.error(error);
                } else {
                    Log.e("IMPBillingManager", "productCallbackContext is null");
                }
            }
        });
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        switch (action) {
            case "getProducts":
                productCallbackContext = callbackContext; // Initialize productCallbackContext
                billingManager.getProducts();
                return true;
            case "setIds":
                JSONArray jsonIds = args.getJSONArray(0);
                if (jsonIds != null) {
                    setIds(jsonIds);
                }
                return true;
            case "buyProduct":
                String oldSku = null;
                if (args.length() > 1) {
                    oldSku = args.getString(1);
                }
                billingManager.buyProduct(args.getString(0), cordova.getActivity(), oldSku);
                return true;
            case "canMakePayments":
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, billingManager.canMakePurchase));
                return true;
            case "onUpdate":
                updateCallbackContext = callbackContext;
                return true;
            case "refreshStatus":
                billingManager.refreshStatus();
                return true;
            case "setValidation":
                billingManager.setValidation(args.getString(0), args.getString(1), args.getString(2));
                return true;
            case "manageSubscriptions":
                try {
                    Uri playStoreUri = Uri.parse("https://play.google.com/store/account/subscriptions");
                    Intent manageIntent = new Intent(Intent.ACTION_VIEW, playStoreUri);
                    this.cordova.getActivity().startActivity(manageIntent);
                    callbackContext.success();
                } catch (Exception e) {
                    e.printStackTrace();
                    callbackContext.error(e.getLocalizedMessage());
                }
                return true;
            default:
                callbackContext.error("\"" + action + "\" is not a recognized action.");
                break;
        }
        return false;
    }

    private void setIds(@NonNull JSONArray jsonArray) {
        ArrayList<String> listIds = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                if (jsonArray.getString(i) != null && !jsonArray.getString(i).isEmpty()) {
                    listIds.add(jsonArray.getString(i));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        billingManager.setIDs(listIds);
    }

    private void sendUpdateMessage(IMPUpdateMessage message, PluginResult.Status status) {
        if (updateCallbackContext != null) {
            Gson gson = new Gson();
            PluginResult result = new PluginResult(status, gson.toJson(message));
            result.setKeepCallback(true);
            updateCallbackContext.sendPluginResult(result);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        billingManager.endBilling();
    }
}
