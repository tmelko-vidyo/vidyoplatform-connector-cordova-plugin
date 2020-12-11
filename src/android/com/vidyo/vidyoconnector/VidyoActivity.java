package com.vidyo.vidyoconnector;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.vidyo.VidyoClient.Connector.Connector;
import com.vidyo.VidyoClient.Connector.ConnectorPkg;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.concurrent.atomic.AtomicBoolean;

import com.vidyo._package.name.R;

public class VidyoActivity extends Activity implements Connector.IConnect, ViewTreeObserver.OnGlobalLayoutListener {

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
        findViewById(R.id.cameraPrivacyButton).setOnClickListener(this::cameraPrivacyStateChanged);
        findViewById(R.id.microphonePrivacyButton).setOnClickListener(this::micPrivacyStateChanged);
        findViewById(R.id.cameraSwitch).setOnClickListener(v -> cycleCameraPressed());

        ConnectorPkg.initialize();
        ConnectorPkg.setApplicationUIContext(this);

        mVidyoConnector = new Connector(mVideoFrame,
                Connector.ConnectorViewStyle.VIDYO_CONNECTORVIEWSTYLE_Default,
                16,
                "debug@VidyoClient info@VidyoConnector warning ",
                Logger.configLogFile(getApplicationContext()),
                0);

        Logger.i("Version is " + mVidyoConnector.getVersion());

        ViewTreeObserver viewTreeObserver = mVideoFrame.getViewTreeObserver();
        if (viewTreeObserver.isAlive()) {
            viewTreeObserver.addOnGlobalLayoutListener(this);
        }

        // If the app was launched by a different app, then get any parameters; otherwise use default settings
        Intent intent = getIntent();
        if (intent != null) {
            connectData = new ConnectData(intent.hasExtra("portal") ? intent.getStringExtra("portal") : "*.vidyocloud.com",
                    intent.hasExtra("roomKey") ? intent.getStringExtra("roomKey") : "",
                    intent.hasExtra("pin") ? intent.getStringExtra("pin") : "",
                    intent.hasExtra("displayName") ? intent.getStringExtra("displayName") : "");
            mToggleConnectButton.setEnabled(true);
        } else {
            mToggleConnectButton.setEnabled(false);
        }
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
    public void onActionRequested(TriggerAction triggerAction) {
        switch (triggerAction) {
            case DISCONNECT:
                if (mVidyoConnector != null) {
                    triggerDisconnect();
                }
                break;
            case RELEASE:
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

            final boolean status = mVidyoConnector.connectToRoomAsGuest(
                    connectData.portal,
                    connectData.displayName,
                    connectData.room,
                    connectData.pin,
                    this);

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

        EventBus.getDefault().post(EventAction.reportEvent(EventAction.MIC_STATE_EVENT, privacy));
    }

    // Toggle the camera privacy
    public void cameraPrivacyStateChanged(View view) {
        mIsCameraPrivacyOn = ((ToggleButton) view).isChecked();
        mVidyoConnector.setCameraPrivacy(mIsCameraPrivacyOn);

        EventBus.getDefault().post(EventAction.reportEvent(EventAction.CAMERA_STATE_EVENT, mIsCameraPrivacyOn));
    }

    // Handle the camera swap button being pressed. Cycle the camera.
    public void cycleCameraPressed() {
        mVidyoConnector.cycleCamera();
    }

    /*
     *  Connector Events
     */

    @Override
    public void onSuccess() {
        EventBus.getDefault().post(EventAction.reportEvent(EventAction.CONNECTED_EVENT, null));

        Logger.i("OnSuccess: successfully connected.");
        ConnectorStateUpdated(VidyoInternalState.VC_CONNECTED, "Connected");
    }

    @Override
    public void onFailure(Connector.ConnectorFailReason reason) {
        EventBus.getDefault().post(EventAction.reportEvent(EventAction.FAILURE_EVENT, reason.name()));

        Logger.i("OnFailure: connection attempt failed, reason = " + reason.toString());

        // Update UI to reflect connection failed
        ConnectorStateUpdated(VidyoInternalState.VC_CONNECTION_FAILURE, "Connection failed");
    }

    @Override
    public void onDisconnected(Connector.ConnectorDisconnectReason reason) {
        EventBus.getDefault().post(EventAction.reportEvent(EventAction.DISCONNECTED_EVENT, reason.name()));

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
}

class ConnectData {

    String portal;
    String room;
    String pin;
    String displayName;

    public ConnectData(String portal, String room, String pin, String displayName) {
        this.portal = portal;
        this.room = room;
        this.pin = pin;
        this.displayName = displayName;
    }
}