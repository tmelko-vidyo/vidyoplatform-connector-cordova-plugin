package com.vidyo.plugin;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Toast;

import com.vidyo.vidyoconnector.EventAction;
import com.vidyo.vidyoconnector.Logger;
import com.vidyo.vidyoconnector.VidyoActivity;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * This class echoes a string called from JavaScript.
 */
public class VidyoPlugin extends CordovaPlugin {

  private static final int PERMISSION_REQ_CODE = 0x7b;

  private static final String[] PERMISSIONS = new String[]{
          Manifest.permission.CAMERA,
          Manifest.permission.RECORD_AUDIO
  };

  private JSONArray connectArguments;

  private CallbackContext pluginCallback;

  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    super.initialize(cordova, webView);
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    Logger.i("Received action from JS layer: " + action);

    switch (action) {
      case EventAction.CONNECT:
        /* Register to vidyo activity events */
        if (!EventBus.getDefault().hasSubscriberForEvent(EventAction.class)) {
          EventBus.getDefault().register(this);
        }

        /* Store JS callback point */
        this.pluginCallback = callbackContext;

        this.openConference(args);
        return true;
      case EventAction.DISCONNECT:
        /* Deliver disconnect action to plugin's activity */
        EventBus.getDefault().post(EventAction.report(EventAction.DISCONNECT));
        return true;

      case EventAction.RELEASE:
        /* Send release action to plugin's activity */
        EventBus.getDefault().post(EventAction.report(EventAction.RELEASE));
        return true;

      case EventAction.SET_PRIVACY:
        Bundle privacyData = new Bundle();
        privacyData.putString(EventAction.DEVICE_KEY, (String) args.get(0));
        privacyData.putBoolean(EventAction.PRIVACY_KEY, (boolean) args.get(1));

        EventBus.getDefault().post(EventAction.report(EventAction.SET_PRIVACY, privacyData));
        return true;

      case EventAction.SELECT_DEFAULT_DEVICE:
        Bundle selectDefaultDevice = new Bundle();
        selectDefaultDevice.putString(EventAction.DEVICE_KEY, (String) args.get(0));

        EventBus.getDefault().post(EventAction.report(EventAction.SELECT_DEFAULT_DEVICE, selectDefaultDevice));
        return true;

      case EventAction.CYCLE_CAMERA:
        EventBus.getDefault().post(EventAction.report(EventAction.CYCLE_CAMERA));
        return true;
    }

    return false;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();

    /* Unregister from vidyo activity events */
    if (EventBus.getDefault().hasSubscriberForEvent(EventAction.class)) {
      EventBus.getDefault().unregister(this);
    }
  }

  private void openConference(JSONArray args) throws JSONException {
    Context context = cordova.getActivity().getApplicationContext();

    /* Check for required permissions */
    if (!hasAllPermissions()) {
      this.connectArguments = args;
      this.cordova.requestPermissions(this, PERMISSION_REQ_CODE, PERMISSIONS);
      return;
    }

    Intent intent = new Intent(context, VidyoActivity.class);

	boolean isPlatform = args.getInt(0) == 1;
    intent.putExtra("isPlatform", isPlatform);

    if (isPlatform) {
      intent.putExtra("portal", args.getString(1));
      intent.putExtra("roomKey", args.getString(2));
      intent.putExtra("pin", args.getString(3));
    } else {
      intent.putExtra("host", args.getString(1));
      intent.putExtra("token", args.getString(2));
      intent.putExtra("resource", args.getString(3));
    }

    intent.putExtra("displayName", args.getString(4));
    intent.putExtra("participants", args.getInt(5));
    intent.putExtra("logLevel", args.getString(6));

    this.cordova.getActivity().startActivity(intent);
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onVidyoEvent(EventAction eventAction) {
    if (pluginCallback != null) {
      PluginResult result = new PluginResult(PluginResult.Status.OK, eventAction.getJsonBody());
      result.setKeepCallback(true);
      pluginCallback.sendPluginResult(result);

      Logger.i("Event reported: " + eventAction.getJsonBody());
    } else {
      Logger.e("JS callback context is null.");
    }
  }

  @Override
  public void onRequestPermissionResult(int requestCode, String[] permissions, int[] grantResults) throws JSONException {
    if (requestCode == PERMISSION_REQ_CODE) {
      for (int result : grantResults) {
        if (result == PackageManager.PERMISSION_DENIED) {
          Toast.makeText(cordova.getActivity(), "Permissions are not granted!", Toast.LENGTH_SHORT).show();
          return; /* quit */
        }
      }

      /* Success */
      if (connectArguments != null) {
        this.openConference(connectArguments);
        this.connectArguments = null;
      }
    }
  }

  private boolean hasAllPermissions() {
    for (String permission : PERMISSIONS) {
      if (!this.cordova.hasPermission(permission)) return false;
    }

    return true;
  }
}