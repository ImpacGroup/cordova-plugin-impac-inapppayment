# README #

### Cordova Plugin for iOS and Android to buy InApp payment subscriptions. ###

## Supported platforms
- Android 6+
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

The purchase process is complete asynchron, therefor you need to listen to updates with *onupdate*. 

```js 
window.plugins.impacInappPayment.onUpdate((result) => {
        //Example
        const json = JSON.parse(result);
        const message = plainToClass(UpdateMessage, json as any);
        message.products;
        message.status;
        message.description;
        message.transactions;
    }, (error) => {
        const json = JSON.parse(error);
        const message = plainToClass(UpdateMessage, json as any);
        message.products;
        message.status;
        message.description;
        message.transactions;
    });
```

## Load Products

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

## Buy product

Before you buy a product make sure the user is entitled to purchase products and the app can communicate with the store.

```js
window.plugins.impacInappPayment.canMakePayments((canMake) => {
    if (canMake) {
        // Purchase products
    }
})
```

To purchase a product perform *buyProduct* with the product id. If you want to upgrade or downgrade a subscription on android, make sure to also add the old sku. Otherwise you create a new subscrition. 

```js
window.plugins.impacInappPayment.buyProduct(productId, oldSku)
```

## Status of subscriptions

You can refresh the status of your subscriptions manually by calling:

```js
window.plugins.impacInappPayment.refreshStatus()
```

This will check if there are changes of your subscriptions and will send them for validation to your server. Please note that this mechanism will automatically run daily for android. For iOS this runs only if a subscrition changes.

## Manage subscriptions

It's best practice to make is as easy as possible for the users to manage there subscriptions.
With the following function you can open the subscrition management of android or ios.

```js
window.plugins.impacInappPayment.manageSubscriptions(onSuccess, onFail)
```
