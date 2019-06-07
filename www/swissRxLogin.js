

// Empty constructor
function SwissRxLogin() {}

SwissRxLogin.prototype.showLogin = function(companyId, appID, successCallback, errorCallback) {
    cordova.exec(successCallback, this._getErrorCallback(errorCallback, "showLogin"), 'SwissRxLogin', 'showLogin', [companyId, appID]);
}

SwissRxLogin.prototype._getErrorCallback = function (ecb, functionName) {
    if (typeof ecb === 'function') {
        return ecb;
    } else {
        return function (result) {
            console.log("The injected error callback of '" + functionName + "' received: " + JSON.stringify(result));
          }
    }
};

SwissRxLogin.install = function() {
    if (!window.plugins) {
        window.plugins = {};
    }
    window.plugins.swissRxLogin = new SwissRxLogin();
    return window.plugins.swissRxLogin;
}
cordova.addConstructor(SwissRxLogin.install);
