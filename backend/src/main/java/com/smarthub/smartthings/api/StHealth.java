package com.smarthub.smartthings.api;

/** Risposta di GET /devices/{deviceId}/health. state: ONLINE / OFFLINE / UNKNOWN. */
public record StHealth(String deviceId, String state) {

    public boolean isOnline() {
        return !"OFFLINE".equalsIgnoreCase(state);
    }
}
