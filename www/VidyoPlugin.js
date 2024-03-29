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
 * 
 * Ptatform Arguments:
 * 0: isPlatform [number] true - 1 | false - 0
 * 1: Portal [string]
 * 2: Room Key [string]
 * 3: Pin [string]
 * 4: Display Name [string]
 * 5: Max Participants [number]
 * 6: Log Level [string]
 * 7: Auto Join [number] true - 1 | false - 0
 * 
 * Vidyo.io Arguments:
 * 0: isPlatform [number] true - 1 | false - 0
 * 1: Host [string]
 * 2: Token [string]
 * 3: Resource [string]
 * 4: Display Name [string]
 * 5: Max Participants [number]
 * 6: Log Level [string]
 * 7: Auto Join [number] true - 1 | false - 0
 * 
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

/*
 * Manage camera|mic|speaker privacy from JS layer
 * Call it:
 * VidyoPlugin.setPrivacy(["camera | mic | speaker", true | false]);
 */
VidyoPlugin.prototype.setPrivacy = function(args) {
    exec(function(){}, nativeErrorCallback, "VidyoPlugin", "setPrivacy", args);
}

/*
 * Select default camera|mic|speaker device from JS layer
 * Call it:
 * VidyoPlugin.selectDefaultDevice(["camera | mic | speaker"]);
 */
VidyoPlugin.prototype.selectDefaultDevice = function(args) {
    exec(function(){}, nativeErrorCallback, "VidyoPlugin", "selectDefaultDevice", args);
}

/*
 * Cycle camera from JS layer
 * Call it:
 * VidyoPlugin.cycleCamera();
 */
VidyoPlugin.prototype.cycleCamera = function(args) {
    exec(function(){}, nativeErrorCallback, "VidyoPlugin", "cycleCamera", null);
}

module.exports = new VidyoPlugin();
