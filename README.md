# README #

### Cordova Plugin for iOS and Android to buy InApp payment subscriptions. ###

## Supported platforms
- Android 5+
- iOS 11+

## Install
To install the cordova plugin perform:
'cordova plugin add cordova-plugin-impac-inapppayment'

## Basic

The plugin creates the object `window.plugins.impacInappPayment` and is accessible after *deviceready* has been fired.

For iOS make sure to set the product ids. Make sure to do this as early as possible. It would be best to perfom it directly after *deviceready*.

```js
window.plugins.impacInappPayment.setIds(ids: ["PRODUCT_ID"]);
```

After setting the ids you have to set the validation configuration for your server. The configurations expects a valid token, the url of the endpoint and a string for the *authorizationType* this could be "Bearer" or "Basic".
```js
window.plugins.impacInappPayment.setValidation(
    token: "YOUR_AUTHENTICATION_TOKEN"
    url: "URL_ENDPOINT",
    authorizationType: "Bearer or Basic" 
)
```


# Load Products

Load the available products with *getProductList*. Make sure you've set the ids before in ios.
```js 
window.plugins.impacInappPayment.getProductList(onSuccess, onFail)

function onSuccess(inAppProducts) {

    //Example
    inAppProducts[0].id
    inAppProducts[0].localeCode
    inAppProducts[0].currency
    inAppProducts[0].localizedDescription
    inAppProducts[0].localizedTitle
    inAppProducts[0].price
}

function onFail(error) {
    alert('Failed because: ' + error);
}
```
