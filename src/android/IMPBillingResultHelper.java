package de.impacgroup.inapppayment;

import com.android.billingclient.api.BillingClient;

public class IMPBillingResultHelper {
    public static String getDescriptionFor(int code) {
        return switch (code) {
            case BillingClient.BillingResponseCode.BILLING_UNAVAILABLE ->
                    "Billing API version is not supported for the type requested";
            case BillingClient.BillingResponseCode.DEVELOPER_ERROR ->
                    "Invalid arguments provided to the API.";
            case BillingClient.BillingResponseCode.ERROR -> "Fatal error during the API action";
            case BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED ->
                    "Requested feature is not supported by Play Store on the current device.";
            case BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED ->
                    "Failure to purchase since item is already owned";
            case BillingClient.BillingResponseCode.ITEM_NOT_OWNED ->
                    "Failure to consume since item is not owned";
            case BillingClient.BillingResponseCode.ITEM_UNAVAILABLE ->
                    "Requested product is not available for purchase";
            case BillingClient.BillingResponseCode.SERVICE_DISCONNECTED ->
                    "Play Store service is not connected now - potentially transient state.";
            case BillingClient.BillingResponseCode.SERVICE_TIMEOUT ->
                    "The request has reached the maximum timeout before Google Play responds.";
            case BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE ->
                    "Network connection is down";
            case BillingClient.BillingResponseCode.USER_CANCELED ->
                    "User pressed back or canceled a dialog";
            default -> "";
        };
    }
}
