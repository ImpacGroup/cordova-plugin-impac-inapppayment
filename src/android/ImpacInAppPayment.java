package de.impacgroup.inapppayment;

import android.content.Context;

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

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        final Context context = this.cordova.getActivity().getApplicationContext();
        billingManager = new IMPBillingManager(context);
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
                String json = new Gson().toJson(list);
                productCallbackContext.success(json);
            }

            @Override
            public void failedLoadingProducts(String error) {
                productCallbackContext.error(error);
            }
        });
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        switch (action) {
            case "getProducts":
                billingManager.getProducts();
                productCallbackContext = callbackContext;
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
                callbackContext.success(1);
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

    private void finishProcess(@Nullable String error) {
        if (updateCallbackContext != null) {
            PluginResult result = new PluginResult( error == null ? PluginResult.Status.OK : PluginResult.Status.ERROR);
            result.setKeepCallback(true);
            updateCallbackContext.sendPluginResult(result);
        }
    }

    private void sendUpdateMessage(IMPUpdateMessage message, PluginResult.Status status) {
        if (updateCallbackContext != null) {
            Gson gson = new Gson();
            PluginResult result = new PluginResult( status, gson.toJson(message));
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
