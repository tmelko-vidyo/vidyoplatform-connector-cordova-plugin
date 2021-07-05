# vidyoplatform-connector-cordova

This is a VidyoPlatform Android and iOS plugin for Cordova. 

## How to use

      cordova plugin add <path-to-plugin-folder>


## How to import this plugin in to a Cordova Project

Here we demontrate how to create a sample Cordova project and import the VidyoPlugin in to that project.

### Prerequisites

- npm - Node.js package manager. I have used NodeJS version 6.9.1. Download from - https://nodejs.org/en/
- Android SDK with Android API 26 (v8.0) installed.
- AndroidStudio if you want to debug the application

### Install Cordova
      $ npm install -g cordova [I have used Cordova 7.0.1]
      
### Create a Cordova project
      $ cordova create VidyoPlatformConnector com.vidyo.platform.connector "VidyoPlatformConnector app"

### Add Android & iOS platform to this project
      $ cd VidyoPlatformConnector
      $ cordova platform add android
      $ cordova platform add ios
    
### Add VidyoClient SDK   
1. Download and unzip Android & iOS https://developer.vidyo.io/#/packages
2. Copy /VidyoClient-AndroidSDK/lib/android content to /vidyoplatform-connector-cordova-plugin/src/android/lib/
3. Copy /VidyoClient-iOSSDK/lib/ios/VidyoClientIOS.framework to /vidyoplatform-connector-cordova-plugin/src/ios

### Now add the previously built VidyoPlugin to this project
      $ cordova plugin add <plugin-path>  

This step copies all the relevant files from the plugin folder to the Cordova project folder. It also merges the information related to permissions in to the AndroidManifest.xml. This step should complete without any errors.

### Make changes to Cordova application UI
Cordova application's main page is rendered using an html file - VidyoPlatformConnector/www/index.html. Now we add a button to this html page. Clicking this button will launch the native Vidyo conference activity. We also add text boxed to collect the information like vidyo portal, room key, display name and pin if.

```
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Security-Policy" content="default-src 'self' data: gap: https://ssl.gstatic.com 'unsafe-eval'; style-src 'self' 'unsafe-inline'; media-src *; img-src 'self' data: content:;">
<meta name="format-detection" content="telephone=no">
<meta name="msapplication-tap-highlight" content="no">
<meta name="viewport" content="user-scalable=no, initial-scale=1, maximum-scale=1, minimum-scale=1, width=device-width">
<link rel="stylesheet" type="text/css" href="css/index.css">
<title>Hello World</title>
</head>
<body>
<div class="app">
<h1>Apache Cordova</h1>
<div id="deviceready" class="blink">
<p class="event listening">Connecting to Device</p>
<p class="event received">Device is Ready</p>
</div>
<p>
<label>Portal:</label>
<input type = "text" id = "portal" value = "*.vidyocloudstaging.com" />
</p>
<p>
<label>Room Key:</label>
<input type = "text" id = "roomKey" value = "required" />
</p>
<p>
<label>Disaply Name:</label>
<input type = "text" id = "displayName" value = "your name" />
</p>
<p>
<label>Pin:</label>
<input type = "text" id = "pin" value = "" />
</p>
<button id = "open_conference">Conference</button>
</div>
<script type="text/javascript" src="cordova.js"></script>
<script type="text/javascript" src="js/index.js"></script>
</body>
</html>

```
Next edit the VidyoPlatformConnector/www/js/index.js and define the onclick event for the button we just added.

```
var app = {
    // Application Constructor
    initialize: function() {
        document.addEventListener('deviceready', this.onDeviceReady.bind(this), false);
    },

    // deviceready Event Handler
    //
    // Bind any cordova events here. Common events are:
    // 'pause', 'resume', etc.
    onDeviceReady: function() {
        this.receivedEvent('deviceready');
    },

    // Update DOM on a Received Event
    receivedEvent: function(id) {
        var parentElement = document.getElementById(id);
        var listeningElement = parentElement.querySelector('.listening');
        var receivedElement = parentElement.querySelector('.received');

        listeningElement.setAttribute('style', 'display:none;');
        receivedElement.setAttribute('style', 'display:block;');

        console.log('Received Event: ' + id);

        document.getElementById("open_conference").addEventListener("click", connect);
    }
};

app.initialize();

var onVidyoEvent = {

    onEvent: function(response) {
      var event = response.event;

      switch (event) {
        case "Connected":
          console.log("JS layer: connected to the conference");
          break;

        case "Disconnected":
          let disconnectReason = response.value;
          console.log(
            "JS layer: disconnected from the conference. Reason: " +
              disconnectReason
          );
          break;

        case "Failure":
          let failureReason = response.value;
          console.log(
            "JS layer: Failure during connection. Reason: " + failureReason
          );
          break;

        case "CameraStateUpdated":
          let cameraState = response.state;
          console.log(
            "JS layer: Received camera state updated. Muted: " + cameraState
          );
          break;

        case "MicrophoneStateUpdated":
          let micState = response.state;
          console.log(
            "JS layer: Received microphone state updated. Muted: " + micState
          );
          break;

        case "ParticipantJoined":
          let joined = response.participant;
          console.log("JS layer: Participant joined: " + joined);
          break;

        case "ParticipantLeft":
          let left = response.participant;
          console.log("JS layer: Participant left: " + left);
          break;
      }
    }
}

function connect() {
    /* Pass the callback to the native side */
    VidyoPlugin.setCallback(onVidyoEvent);
    
    const isPlatform = 1; // true
    
    var portal = document.getElementById("portal").value;
    var roomKey = document.getElementById("roomKey").value;
    var pin = document.getElementById("pin").value;

    var displayName = document.getElementById("displayName").value;  
    
    VidyoPlugin.connect([isPlatform, portal, roomKey, pin, displayName, 8 /* max visible participants */, "debug@VidyoClient info@VidyoConnector warning" /* debug log level */]);
}

/* Call this methond in order to connect with Vidyo.io */
function connectIO() {
    /* Pass the callback to the native side */
    VidyoPlugin.setCallback(onVidyoEvent);
    
    const isPlatform = 0; // false
    
    var host = "prod.vidyo.io";
    var token = "generated-token";
    var resource = "demoRoom";

    var displayName = "display-name";  
    
    VidyoPlugin.connect([isPlatform, host, token, resource, displayName, 8 /* max visible participants */, "debug@VidyoClient info@VidyoConnector warning" /* debug log level */]);
}

/*
 * Disconnect from the call from JS layer. 
 * Would fallback into 'Disconnected' callback 
 */
function disconnect() {
    console.log("JS layer: disconnect requested");
    VidyoPlugin.disconnect();
}

/* 
 * Release and close native plugin from JS layer. 
 * You have to disconnect first. This methond won't work otherwise.
 */
function release() {
    console.log("JS layer: release requested");
    VidyoPlugin.release();
}

```
#### Android

Build the project

      $ cd VidyoPlatformConnector
      $ cordova build android

If the build is successful you can run the application

      $cordova run android
      
You can also run the application by manually installing the apk file from VidyoPlatformConnector\platforms\android\build\outputs\apk
On the welcome screen, click on "Conference" button to open the video conference android activity

#### iOS

Build the project

      $ cd VidyoPlatformConnector
      $ cordova build ios

If the build is successful you can run the application

      $cordova run ios
