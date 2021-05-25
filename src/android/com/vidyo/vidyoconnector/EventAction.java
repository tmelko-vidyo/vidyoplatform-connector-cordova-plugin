package com.vidyo.vidyoconnector;

import android.os.Bundle;

import org.json.JSONObject;

public class EventAction {

    /* RECEIVE ACTIONS */
    public static final String CONNECTED_EVENT = "Connected";
    public static final String DISCONNECTED_EVENT = "Disconnected";
    public static final String FAILURE_EVENT = "Failure";

    public static final String CAMERA_STATE_EVENT = "CameraStateUpdated";
    public static final String MIC_STATE_EVENT = "MicrophoneStateUpdated";

    public static final String PARTICIPANT_JOINED = "ParticipantJoined";
    public static final String PARTICIPANT_LEFT = "ParticipantLeft";

    /* SEND ACTIONS */
    public static final String CONNECT = "connect";
    public static final String DISCONNECT = "disconnect";
    public static final String RELEASE = "release";

    public static final String SET_PRIVACY = "setPrivacy";
    public static final String SELECT_DEFAULT_DEVICE = "selectDefaultDevice";
    public static final String CYCLE_CAMERA = "cycleCamera";

    /* PARAMS */
    private static final String EVENT_KEY = "event";

    private static final String VALUE_KEY = "value";
    private static final String STATE_KEY = "state";

    public static final String DEVICE_KEY = "device";
    public static final String PRIVACY_KEY = "privacy";

    public static final String CAMERA_KEY = "camera";
    public static final String MIC_KEY = "mic";
    public static final String SPEAKER_KEY = "speaker";

    private static final String PARTICIPANT_KEY = "participant";

    private final JSONObject jsonBody;

    private EventAction(JSONObject jsonBody) {
        this.jsonBody = jsonBody;
    }

    public JSONObject getJsonBody() {
        return jsonBody;
    }

    public String getEvent() {
        return jsonBody.optString(EVENT_KEY);
    }

    public static EventAction report(String event) {
        return report(event, new Bundle());
    }

    public static EventAction report(String event, boolean state) {
        Bundle bundle = new Bundle();
        bundle.putBoolean(STATE_KEY, state);
        return report(event, bundle);
    }

    public static EventAction report(String event, String value) {
        Bundle bundle = new Bundle();
        bundle.putString(VALUE_KEY, value);
        return report(event, bundle);
    }

    public static EventAction report(String event, JSONObject participant) {
        Bundle bundle = new Bundle();
        bundle.putString(PARTICIPANT_KEY, participant.toString());
        return report(event, bundle);
    }

    public static EventAction report(String event, Bundle data) {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(EVENT_KEY, event);

            if (data != null)
                for (String key : data.keySet()) jsonObject.put(key, data.get(key));

            return new EventAction(jsonObject);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }
}