

// Empty constructor
function ImpacInappPayment() {}

ImpacInappPayment.prototype.getProducts = function(successCallback, errorCallback) {
    cordova.exec(successCallback, this._getErrorCallback(errorCallback, "getProducts"), 'ImpacInappPayment', 'getProducts', [companyId, appID]);
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
