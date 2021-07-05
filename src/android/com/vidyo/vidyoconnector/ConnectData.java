package com.vidyo.vidyoconnector;

class ConnectData {

    boolean isPlatform;

    /* Platform */
    String portal;
    String room;
    String pin;

    /* IO */
    String host;
    String token;
    String resource;

    String displayName;

    public void populatePlatform(String portal, String room, String pin, String displayName) {
        this.portal = portal;
        this.room = room;
        this.pin = pin;
        this.displayName = displayName;
        this.isPlatform = true;
    }

    public void populateIO(String host, String token, String resource, String displayName) {
        this.host = host;
        this.token = token;
        this.resource = resource;
        this.displayName = displayName;
        this.isPlatform = false;
    }
}