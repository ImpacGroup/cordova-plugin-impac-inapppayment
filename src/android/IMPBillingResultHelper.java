package de.impacgroup.inapppayment;

import com.android.billingclient.api.BillingClient;

public class IMPBillingResultHelper {
    public static String getDescriptionFor(int code) {
        switch (code) {
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE:
                return "Billing API version is not supported for the type requested";
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR:
                return "Invalid arguments provided to the API.";
            case BillingClient.BillingResponseCode.ERROR:
                return "Fatal error during the API action";
            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED:
                return "Requested feature is not supported by Play Store on the current device.";
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED:
                return "Failure to purchase since item is already owned";
            case BillingClient.BillingResponseCode.ITEM_NOT_OWNED:
                return "Failure to consume since item is not owned";
            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE:
                return "Requested product is not available for purchase";
            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED:
                return "Play Store service is not connected now - potentially transient state.";
            case BillingClient.BillingResponseCode.SERVICE_TIMEOUT:
                return "The request has reached the maximum timeout before Google Play responds.";
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE:
                return "Network connection is down";
            case BillingClient.BillingResponseCode.USER_CANCELED:
                return "User pressed back or canceled a dialog";
        }
        return "";
    }
}
