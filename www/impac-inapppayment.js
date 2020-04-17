

// Empty constructor
function ImpacInappPayment() {}

ImpacInappPayment.prototype.setIds = function(ids) {
    cordova.exec(null, null, 'ImpacInappPayment', 'setIds', [ids]);
}

ImpacInappPayment.prototype.setValidation = function(accessToken, url) {
    cordova.exec(null, null, 'ImpacInappPayment', 'setValidation', [accessToken, url]);
}

ImpacInappPayment.prototype.getProducts = function(successCallback) {
    cordova.exec(successCallback, null, 'ImpacInappPayment', 'getProducts', []);
}

ImpacInappPayment.prototype.buyProduct = function(productID, oldSku) {
    if (oldSku) {
        cordova.exec(null, null, 'ImpacInappPayment', 'buyProduct', [productID, oldSku]);
    } else {
        cordova.exec(null, null, 'ImpacInappPayment', 'buyProduct', [productID]);
    }
}

ImpacInappPayment.prototype.onUpdate = function(successCallback) {
    cordova.exec(successCallback, null, 'ImpacInappPayment', 'onUpdate', []);
}

ImpacInappPayment.prototype.restorePurchases = function() {
  cordova.exec(null, null, 'ImpacInappPayment', 'restorePurchases', []);
}

ImpacInappPayment.prototype.canMakePayments = function(successCallback) {
    cordova.exec(successCallback, null, 'ImpacInappPayment', 'canMakePayments', []);
}

ImpacInappPayment.prototype.refreshStatus = function() {
    cordova.exec(null, null, 'ImpacInappPayment', 'refreshStatus', []);
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
