package de.impacgroup.inapppayment;

import androidx.annotation.Nullable;

import com.android.billingclient.api.SkuDetails;

public class IMPProduct {
    public String id;
    public String localizedTitle;
    public String localizedDescription;
    public String price;
    public @Nullable String localeCode;
    public String currency;

    IMPProduct(SkuDetails skuDetails) {
        this.id = skuDetails.getSku();
        this.price = skuDetails.getPrice();
        this.localizedTitle = skuDetails.getTitle();
        this.localizedDescription = skuDetails.getDescription();
        this.currency = skuDetails.getPriceCurrencyCode();

    }
}
