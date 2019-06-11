package de.impacgroup.swissrxlogin;


import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import static org.apache.cordova.Whitelist.TAG;

public class SwissRxLogin extends CordovaPlugin {
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("showLogin")) {
            Log.d(TAG, "execute: " + action);
            return super.execute(action, args, callbackContext);
        } else {
            callbackContext.error("\"" + action + "\" is not a recognized action.");
            return false;
        }

    }
}

