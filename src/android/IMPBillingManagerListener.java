package de.impacgroup.inapppayment;

import java.util.List;

public interface IMPBillingManagerListener {
    void finishedPurchase(String sku);
    void pendingPurchase(String sku);
    void failedPurchase(String error);
    void productsLoaded(List<IMPProduct> list);
    void failedLoadingProducts(String error);
}
