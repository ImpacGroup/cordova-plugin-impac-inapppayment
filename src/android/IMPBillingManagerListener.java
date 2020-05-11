package de.impacgroup.inapppayment;

import androidx.annotation.Nullable;

import java.util.List;

public interface IMPBillingManagerListener {
    void finishedPurchase(String sku);
    void pendingPurchase(String sku);
    void failedPurchase(@Nullable String error);
    void productsLoaded(List<IMPProduct> list);
    void failedLoadingProducts(String error);
}
