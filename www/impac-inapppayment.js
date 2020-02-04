

// Empty constructor
function ImpacInappPayment() {}

ImpacInappPayment.prototype.setIds = function(ids) {
    cordova.exec(null, null, 'ImpacInappPayment', 'setIds', [ids]);
}

ImpacInappPayment.prototype.getProducts = function(successCallback) {
    cordova.exec(successCallback, null, 'ImpacInappPayment', 'getProducts', []);
}

ImpacInappPayment.prototype.buyProduct = function(successCallback, errorCallback, productID, accessToken, url) {
    cordova.exec(successCallback, this._getErrorCallback(errorCallback, "buyProduct"), 'ImpacInappPayment', 'buyProduct', [productID, accessToken, url]);
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
