

// Empty constructor
function SwissRxLogin() {}

SwissRxLogin.prototype.show = function(message, successCallback, errorCallback) {
    cordova.exec(successCallback, errorCallback, 'SwissRxLogin', 'show', message);
}

SwissRxLogin.install = function() {
    if (!window.plugins) {
        window.plugins = {};
    }
    window.plugins.swissRxLogin = new SwissRxLogin();
    return window.plugins.swissRxLogin;
}
cordova.addConstructor(SwissRxLogin.install);
