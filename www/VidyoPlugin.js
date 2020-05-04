var exec = require('cordova/exec');

var jsCallback;

function VidyoPlugin() {}

/**
 * Handle callback from native side
 * 
 * @param {JSON} response 
 */
var nativeResponseCallback = function(response) {
    console.log("Received native callback: " + JSON.stringify(response));
    jsCallback.onEvent(response);
}

/**
 * Handle error from native side
 * 
 * @param {*} error 
 */
var nativeErrorCallback = function(error) {
    console.log("Error from native side: " + error);
}

VidyoPlugin.prototype.setCallback = function(callback) {
    console.log("Callback to JS has been provided");
    jsCallback = callback;
}

/**
 * Launch conference activity and pass the callbacks
 */
VidyoPlugin.prototype.connect = function(args) {
    exec(nativeResponseCallback, nativeErrorCallback, "VidyoPlugin", "connect", args);
}

/**
 * Disconnect from the conference
 */
VidyoPlugin.prototype.disconnect = function() {
    console.log("Trigger disconnect on native side.");
    exec(function(){}, nativeErrorCallback, "VidyoPlugin", "disconnect", null);
}

/**
 * Wrap up the plugin screen and release the connector
 */
VidyoPlugin.prototype.release = function() {
    console.log("Trigger release on native side.");
    exec(function(){}, nativeErrorCallback, "VidyoPlugin", "release", null);
}

module.exports = new VidyoPlugin();
