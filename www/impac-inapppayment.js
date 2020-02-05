

// Empty constructor
function ImpacInappPayment() {}

ImpacInappPayment.prototype.setIds = function(ids) {
    cordova.exec(null, null, 'ImpacInappPayment', 'setIds', [ids]);
}

ImpacInappPayment.prototype.getProducts = function(successCallback) {
    cordova.exec(successCallback, null, 'ImpacInappPayment', 'getProducts', []);
}

ImpacInappPayment.prototype.buyProduct = function(productID, accessToken, url) {
    cordova.exec(null, null, 'ImpacInappPayment', 'buyProduct', [productID, accessToken, url]);
}

ImpacInappPayment.prototype.onUpdate = function(successCallback) {
    cordova.exec(successCallback, null, 'ImpacInappPayment', 'onUpdate', []);
}

ImpacInappPayment.prototype._getErrorCallback = function (ecb, functionName) {
    if (typeof ecb === 'function') {
        return ecb;
    } else {
        return function (result) {
            console.log("The injected error callback of '" + functionName + "' received: " + JSON.stringify(result));
          }
    }
};

ImpacInappPayment.install = function() {
    if (!window.plugins) {
        window.plugins = {};
    }
    window.plugins.impacInappPayment = new ImpacInappPayment();
    return window.plugins.impacInappPayment;
}
cordova.addConstructor(ImpacInappPayment.install);
