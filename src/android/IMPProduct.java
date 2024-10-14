package de.impacgroup.inapppayment;

import androidx.annotation.Nullable;

import com.android.billingclient.api.ProductDetails;

public class IMPProduct {
    public String id;
    public String localizedTitle;
    public String localizedDescription;
    public String price;
    public @Nullable String localeCode;
    public String currency;

    IMPProduct(ProductDetails productDetails) {
        this.id = productDetails.getProductId();
        this.localizedTitle = productDetails.getTitle();
        this.localizedDescription = productDetails.getDescription();

        // Check for one-time purchase details
        if (productDetails.getOneTimePurchaseOfferDetails() != null) {
            this.price = productDetails.getOneTimePurchaseOfferDetails().getFormattedPrice();
            this.currency = productDetails.getOneTimePurchaseOfferDetails().getPriceCurrencyCode();
        }
        // Check for subscription details
        else if (productDetails.getSubscriptionOfferDetails() != null && !productDetails.getSubscriptionOfferDetails().isEmpty()) {
            // Assuming you want the first subscription offer details
            ProductDetails.SubscriptionOfferDetails subscriptionOfferDetails = productDetails.getSubscriptionOfferDetails().get(0);
            this.price = subscriptionOfferDetails.getPricingPhases().getPricingPhaseList().get(0).getFormattedPrice();
            this.currency = subscriptionOfferDetails.getPricingPhases().getPricingPhaseList().get(0).getPriceCurrencyCode();
        } else {
            this.price = "N/A";
            this.currency = "N/A";
        }
    }

    @Override
    public String toString() {
        return "IMPProduct{" + "id='" + id + '\'' + ", localizedTitle='" + localizedTitle + '\'' + ", localizedDescription='" + localizedDescription + '\'' + ", price='" + price + '\'' + ", currency='" + currency + '\'' + '}';
    }
}