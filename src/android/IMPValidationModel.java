package de.impacgroup.inapppayment;

public class IMPValidationModel {
    public String purchaseToken;
    public String productId;

    IMPValidationModel(String purchaseToken, String productId) {
        this.productId = productId;
        this.purchaseToken = purchaseToken;
    }
}
