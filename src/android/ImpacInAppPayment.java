package de.impacgroup.inapppayment;

import android.content.Context;

import androidx.annotation.NonNull;

import com.google.gson.Gson;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

public class ImpacInAppPayment extends CordovaPlugin {

    private IMPBillingManager billingManager;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        Context context = this.cordova.getActivity().getApplicationContext();
        billingManager = new IMPBillingManager(context);
    }

    @Override
    public boolean execute(String action, JSONArray args, final CallbackContext callbackContext) throws JSONException {
        if (action.equals("getProducts")) {
            billingManager.getProducts(new IMPBillingManagerProductListener() {
                @Override
                public void productsLoaded(List<IMPProduct> list) {
                    String json = new Gson().toJson(list);
                    callbackContext.success(json);
                }
            });
            return true;
        } else if (action.equals("setIds")) {
            JSONArray jsonIds = args.getJSONArray(0);
            if (jsonIds != null) {
                setIds(jsonIds);
            }
            return true;
        } else if (action.equals("buyProduct")) {
            billingManager.buyProduct(args.getString(0), cordova.getActivity());
            return true;
        } else {
            callbackContext.error("\"" + action + "\" is not a recognized action.");
        }
        return false;
    }

    private void setIds(@NonNull JSONArray jsonArray) {
        ArrayList<String> listIds = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            try {
                listIds.add(jsonArray.getString(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        billingManager.setIDs(listIds);
    }
}
