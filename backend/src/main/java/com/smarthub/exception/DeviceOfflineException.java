package com.smarthub.exception;

/** Il dispositivo risulta offline nell'ultimo stato letto da SmartThings: il comando non viene inviato. */
public class DeviceOfflineException extends RuntimeException {

    private final String deviceId;

    public DeviceOfflineException(String deviceId, String deviceName) {
        super(deviceName + " è offline");
        this.deviceId = deviceId;
    }

    public String deviceId() {
        return deviceId;
    }
}
