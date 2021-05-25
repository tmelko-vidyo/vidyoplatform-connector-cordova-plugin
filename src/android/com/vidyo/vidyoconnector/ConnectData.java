package com.vidyo.vidyoconnector;

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
