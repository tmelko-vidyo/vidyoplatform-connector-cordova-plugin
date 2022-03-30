package com.vidyo.vidyoconnector;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;

import com.vidyo.VidyoClient.Connector.Connector;
import com.vidyo.VidyoClient.Connector.ConnectorPkg;
import com.vidyo.VidyoClient.Endpoint.Participant;
import com.vidyo.platform.connector.R;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

public class VidyoActivity extends Activity implements Connector.IConnect,
        Connector.IRegisterParticipantEventListener,
        ViewTreeObserver.OnGlobalLayoutListener {

  enum VidyoInternalState {
    VC_CONNECTED,
    VC_DISCONNECTED,
    VC_DISCONNECTED_UNEXPECTED,
    VC_CONNECTION_FAILURE
  }

  private VidyoInternalState mVidyoConnectorState = VidyoInternalState.VC_DISCONNECTED;

  private Connector mVidyoConnector = null;

  private FrameLayout mVideoFrame;
  private ToggleButton mToggleConnectButton;
  private TextView mToolbarStatus;

  private ToggleButton mCameraPrivacyButton;
  private ToggleButton mMicrophonePrivacyButton;

  private ConnectData connectData;

  private final AtomicBoolean isDisconnectAndQuit = new AtomicBoolean(false);
  private boolean mIsCameraPrivacyOn = false;

  /*
   *  Operating System Events
   */
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Initialize the VidyoClient
    EventBus.getDefault().register(this);

    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    setContentView(R.layout.activity_main);

    // Initialize the member variables
    mVideoFrame = findViewById(R.id.videoFrame);
    mToggleConnectButton = findViewById(R.id.toggleConnectButton);
    mToolbarStatus = findViewById(R.id.toolbarStatusText);

    mToggleConnectButton.setOnClickListener(v -> toggleConnectDisconnect());

    mCameraPrivacyButton = findViewById(R.id.cameraPrivacyButton);
    mCameraPrivacyButton.setOnClickListener(this::cameraPrivacyStateChanged);

    mMicrophonePrivacyButton = findViewById(R.id.microphonePrivacyButton);
    mMicrophonePrivacyButton.setOnClickListener(this::micPrivacyStateChanged);

    findViewById(R.id.cameraSwitch).setOnClickListener(v -> cycleCameraPressed());

    ConnectorPkg.initialize();
    ConnectorPkg.setApplicationUIContext(this);

    Intent intent = getIntent();

    int maxParticipants = intent.getIntExtra("participants", 8);
    String logLevel = intent.hasExtra("logLevel")
            ? intent.getStringExtra("logLevel")
            : "debug@VidyoClient info@VidyoConnector warning";

    Logger.d("Construct connector with ext data. Max part.: %s, Log level: %s",
            maxParticipants, logLevel);

    mVidyoConnector = new Connector(mVideoFrame,
            Connector.ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default,
            maxParticipants,
            logLevel,
            Logger.configLogFile(getApplicationContext()),
            0);

    Logger.i("Version is " + mVidyoConnector.getVersion());

    mVidyoConnector.registerParticipantEventListener(this);
    mVidyoConnector.reportLocalParticipantOnJoined(true);

    ViewTreeObserver viewTreeObserver = mVideoFrame.getViewTreeObserver();
    if (viewTreeObserver.isAlive()) {
      viewTreeObserver.addOnGlobalLayoutListener(this);
    }

    connectData = new ConnectData();

    boolean isPlatform = intent.hasExtra("isPlatform") && intent.getBooleanExtra("isPlatform", false);
    String displayName = intent.hasExtra("displayName") ? intent.getStringExtra("displayName") : "";

    if (isPlatform) {
      connectData.populatePlatform(intent.hasExtra("portal") ? intent.getStringExtra("portal") : "*.vidyocloud.com",
              intent.hasExtra("roomKey") ? intent.getStringExtra("roomKey") : "",
              intent.hasExtra("pin") ? intent.getStringExtra("pin") : "",
              displayName);
    } else {
      connectData.populateIO(intent.hasExtra("host") ? intent.getStringExtra("host") : "prod.vidyo.io",
              intent.hasExtra("token") ? intent.getStringExtra("token") : "",
              intent.hasExtra("resource") ? intent.getStringExtra("resource") : "",
              displayName);
    }

    mToggleConnectButton.setEnabled(true);
  }

  @Override
  public void onGlobalLayout() {
    RefreshUI();
  }

  @Override
  protected void onStart() {
    super.onStart();
    if (mVidyoConnector != null) {
      mVidyoConnector.setMode(Connector.ConnectorMode.VIDYO_CONNECTORMODE_Foreground);
      mVidyoConnector.setCameraPrivacy(mIsCameraPrivacyOn);
    }
  }

  @Override
  protected void onStop() {
    super.onStop();
    Logger.i("onStop");
    if (mVidyoConnector != null) {
      mVidyoConnector.setMode(Connector.ConnectorMode.VIDYO_CONNECTORMODE_Background);
      mVidyoConnector.setCameraPrivacy(true);
    }
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onActionRequested(EventAction eventAction) {
    switch (eventAction.getEvent()) {
      case EventAction.DISCONNECT:
        if (mVidyoConnector != null) {
          triggerDisconnect();
        }
        break;

      case EventAction.SET_PRIVACY:
        String deviceForPrivacy = eventAction.getJsonBody().optString(EventAction.DEVICE_KEY);
        boolean privacy = eventAction.getJsonBody().optBoolean(EventAction.PRIVACY_KEY);
        handlePrivacyUpdate(deviceForPrivacy, privacy);
        break;

      case EventAction.SELECT_DEFAULT_DEVICE:
        String deviceForDefault = eventAction.getJsonBody().optString(EventAction.DEVICE_KEY);
        handleDefaultDevice(deviceForDefault);
        break;

      case EventAction.CYCLE_CAMERA:
        cycleCameraPressed();
        break;

      case EventAction.RELEASE:
        /* Wrap up the plugin activity by destroying it. This would call onDestroy and release the connector. */
        if (mVidyoConnector != null && mVidyoConnectorState != VidyoInternalState.VC_CONNECTED) {
          finish();
        }
        break;
    }
  }

  @Override
  public void onBackPressed() {
    Connector.ConnectorState state = mVidyoConnector.getState();

    if (state == Connector.ConnectorState.VIDYO_CONNECTORSTATE_Idle || state == Connector.ConnectorState.VIDYO_CONNECTORSTATE_Ready) {
      super.onBackPressed();
    } else {
      /* You are still connecting or connected */
      Toast.makeText(this, "You have to disconnect or await connection first", Toast.LENGTH_SHORT).show();

      /* Start disconnection if connected. Quit afterward. */
      if (state == Connector.ConnectorState.VIDYO_CONNECTORSTATE_Connected && !isDisconnectAndQuit.get()) {
        isDisconnectAndQuit.set(true);
        triggerDisconnect();
      }
    }
  }

  @Override
  protected void onDestroy() {
    Logger.i("onDestroy");
    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    ViewTreeObserver viewTreeObserver = mVideoFrame.getViewTreeObserver();
    if (viewTreeObserver != null) {
      viewTreeObserver.removeOnGlobalLayoutListener(this);
    }

    if (mVidyoConnector != null) {
      mVidyoConnector.unregisterParticipantEventListener();

      mVidyoConnector.hideView(mVideoFrame);
      mVidyoConnector.disable();
      mVidyoConnector = null;
    }

    ConnectorPkg.setApplicationUIContext(null);

    EventBus.getDefault().unregister(this);
    super.onDestroy();
  }

  // The device interface orientation has changed
  @Override
  public void onConfigurationChanged(@NonNull Configuration newConfig) {
    Logger.i("onConfigurationChanged");
    super.onConfigurationChanged(newConfig);

    // Refresh the video size after it is painted
    ViewTreeObserver viewTreeObserver = mVideoFrame.getViewTreeObserver();

    if (viewTreeObserver.isAlive()) {
      viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
          mVideoFrame.getViewTreeObserver().removeOnGlobalLayoutListener(this);

          // Width/height values of views not updated at this point so need to wait
          // before refreshing UI

          RefreshUI();
        }
      });
    }
  }

  /**
   * Handler default device selection
   *
   * @param device camera, mic, speaker
   */
  private void handleDefaultDevice(String device) {
    if (mVidyoConnector == null) return;

    switch (device) {
      case EventAction.CAMERA_KEY:
        mVidyoConnector.selectDefaultCamera();
        break;
      case EventAction.MIC_KEY:
        mVidyoConnector.selectDefaultMicrophone();
        break;
      case EventAction.SPEAKER_KEY:
        mVidyoConnector.selectDefaultSpeaker();
        break;
    }
  }

  /**
   * Handler privacy update event
   *
   * @param device  on specific device camera, mic, speaker
   * @param privacy with privacy state true | false
   */
  private void handlePrivacyUpdate(String device, boolean privacy) {
    switch (device) {
      case EventAction.CAMERA_KEY:
        mVidyoConnector.setCameraPrivacy(privacy);
        mCameraPrivacyButton.setChecked(privacy);
        break;
      case EventAction.MIC_KEY:
        mVidyoConnector.setMicrophonePrivacy(privacy);
        mMicrophonePrivacyButton.setChecked(privacy);
        break;
      case EventAction.SPEAKER_KEY:
        mVidyoConnector.setSpeakerPrivacy(privacy);
        break;
    }
  }

  /*
   * Private Utility Functions
   */

  private void RefreshUI() {
    mVidyoConnector.showViewAt(mVideoFrame, 0, 0, mVideoFrame.getWidth(), mVideoFrame.getHeight());
    Logger.i("VidyoConnectorShowViewAt: x = 0, y = 0, w = " + mVideoFrame.getWidth() + ", h = " + mVideoFrame.getHeight());
  }

  private void ConnectorStateUpdated(VidyoInternalState state, final String statusText) {
    Logger.i("ConnectorStateUpdated, state = " + state.toString());
    mVidyoConnectorState = state;

    runOnUiThread(() -> {
      mToggleConnectButton.setChecked(mVidyoConnectorState == VidyoInternalState.VC_CONNECTED);
      mToolbarStatus.setText(statusText);
    });
  }

  /*
   * Button Event Callbacks
   */

  public void toggleConnectDisconnect() {
    Connector.ConnectorState state = mVidyoConnector.getState();

    if (state == Connector.ConnectorState.VIDYO_CONNECTORSTATE_ConnectingToResource
            || state == Connector.ConnectorState.VIDYO_CONNECTORSTATE_FindingResource
            || state == Connector.ConnectorState.VIDYO_CONNECTORSTATE_EnablingMedia
            || state == Connector.ConnectorState.VIDYO_CONNECTORSTATE_EstablishingConnection) {
      Logger.w("Attempted to interrupt the connection during active process.");
      return;
    }

    if (mToggleConnectButton.isChecked()) {
      mToolbarStatus.setText("Connecting...");

      final boolean status = connectData.isPlatform
              ? mVidyoConnector.connectToRoomAsGuest(connectData.portal, connectData.displayName, connectData.room, connectData.pin, this)
              : mVidyoConnector.connect(connectData.host, connectData.token, connectData.displayName, connectData.resource, this);

      if (!status) {
        ConnectorStateUpdated(VidyoInternalState.VC_CONNECTION_FAILURE, "Connection failed");
      }

      Logger.i("VidyoConnectorConnect status = " + status);
    } else {
      triggerDisconnect();
    }
  }

  private void triggerDisconnect() {
    // The button just switched to the callStart image: The user is either connected to a room
    // or is in the process of connecting to a room; call VidyoConnectorDisconnect to either disconnect
    // or abort the connection attempt.
    // Change the button back to the callEnd image because do not want to assume that the Disconnect
    // call will actually end the call. Need to wait for the callback to be received
    // before swapping to the callStart image.
    mToggleConnectButton.setChecked(true);
    mToolbarStatus.setText("Disconnecting...");
    mVidyoConnector.disconnect();
  }

  // Toggle the microphone privacy
  public void micPrivacyStateChanged(View view) {
    boolean privacy = ((ToggleButton) view).isChecked();
    mVidyoConnector.setMicrophonePrivacy(privacy);

    EventBus.getDefault().post(EventAction.report(EventAction.MIC_STATE_EVENT, privacy));
  }

  // Toggle the camera privacy
  public void cameraPrivacyStateChanged(View view) {
    mIsCameraPrivacyOn = ((ToggleButton) view).isChecked();
    mVidyoConnector.setCameraPrivacy(mIsCameraPrivacyOn);

    EventBus.getDefault().post(EventAction.report(EventAction.CAMERA_STATE_EVENT, mIsCameraPrivacyOn));
  }

  // Handle the camera swap button being pressed. Cycle the camera.
  public void cycleCameraPressed() {
    mVidyoConnector.cycleCamera();
  }

  private JSONObject participantToJSON(Participant participant) {
    JSONObject jsonObject = new JSONObject();
    try {
      jsonObject.put("name", participant.getName());
      jsonObject.put("userId", participant.getUserId());
      jsonObject.put("trust", participant.getTrust());
      jsonObject.put("id", participant.getId());
      jsonObject.put("isLocal", participant.isLocal());
      jsonObject.put("isRecording", participant.isRecording());
      jsonObject.put("applicationType", participant.getApplicationType());
    } catch (JSONException e) {
      e.printStackTrace();
    }

    return jsonObject;
  }

  /*
   *  Connector Events
   */

  @Override
  public void onSuccess() {
    EventBus.getDefault().post(EventAction.report(EventAction.CONNECTED_EVENT));

    Logger.i("OnSuccess: successfully connected.");
    ConnectorStateUpdated(VidyoInternalState.VC_CONNECTED, "Connected");
  }

  @Override
  public void onFailure(Connector.ConnectorFailReason reason) {
    EventBus.getDefault().post(EventAction.report(EventAction.FAILURE_EVENT, reason.name()));

    Logger.i("OnFailure: connection attempt failed, reason = " + reason.toString());

    // Update UI to reflect connection failed
    ConnectorStateUpdated(VidyoInternalState.VC_CONNECTION_FAILURE, "Connection failed");
  }

  @Override
  public void onDisconnected(Connector.ConnectorDisconnectReason reason) {
    EventBus.getDefault().post(EventAction.report(EventAction.DISCONNECTED_EVENT, reason.name()));

    if (reason == Connector.ConnectorDisconnectReason.VIDYO_CONNECTORDISCONNECTREASON_Disconnected) {
      Logger.i("OnDisconnected: successfully disconnected, reason = " + reason.toString());
      ConnectorStateUpdated(VidyoInternalState.VC_DISCONNECTED, "Disconnected");
    } else {
      Logger.i("OnDisconnected: unexpected disconnection, reason = " + reason.toString());
      ConnectorStateUpdated(VidyoInternalState.VC_DISCONNECTED_UNEXPECTED, "Unexpected disconnection");
    }

    /* Wrap up the conference */
    if (isDisconnectAndQuit.get()) {
      finish();
    }
  }

  @Override
  public void onParticipantJoined(Participant participant) {
    EventBus.getDefault().post(EventAction.report(EventAction.PARTICIPANT_JOINED,
            participantToJSON(participant)));
  }

  @Override
  public void onParticipantLeft(Participant participant) {
    EventBus.getDefault().post(EventAction.report(EventAction.PARTICIPANT_LEFT,
            participantToJSON(participant)));
  }

  @Override
  public void onDynamicParticipantChanged(ArrayList<Participant> arrayList) {
  }

  @Override
  public void onLoudestParticipantChanged(Participant participant, boolean b) {
  }
}