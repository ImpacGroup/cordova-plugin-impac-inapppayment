package de.impacgroup.inapppayment;

class IMPValidationConfig {
    public String url;
    public String accessString;
    public String authorizationType;

    IMPValidationConfig(String url, String accessString, String authorizationType) {
        this.url = url;
        this.accessString = accessString;
        this.authorizationType = authorizationType;
    }
}