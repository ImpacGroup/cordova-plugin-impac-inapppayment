package de.impacgroup.swissrxlogin;


import android.content.Context;
import android.content.Intent;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.json.JSONArray;
import org.json.JSONException;

import static android.app.Activity.RESULT_OK;

public class SwissRxLogin extends CordovaPlugin {

    static final String CONST_APPID= "appId";
    static final String CONST_COMPANYID 	= "companyId";

    private CallbackContext callbackContext;

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {

        if (action.equals("showLogin")) {
            showLogin(args.getString(0), args.getString(1));
            this.callbackContext = callbackContext;
            return true;
        } else {
            callbackContext.error("\"" + action + "\" is not a recognized action.");
            return false;
        }

    }

    private void showLogin(final String companyID, final String appId) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                Context context = cordova.getActivity().getApplicationContext();
                cordova.setActivityResultCallback(SwissRxLogin.this);
                Intent intent = new Intent(context, IMPSwissRxActivity.class);
                intent.putExtra(CONST_APPID, appId);
                intent.putExtra(CONST_COMPANYID, companyID);
                cordova.getActivity().startActivityForResult(intent, 0);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == RESULT_OK) {
            callbackContext.success(0);
        } else {
            callbackContext.error(0);
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }
}

