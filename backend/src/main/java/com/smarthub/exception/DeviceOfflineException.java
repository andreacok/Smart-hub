package com.smarthub.exception;

public class DeviceOfflineException extends RuntimeException {
    public DeviceOfflineException(String deviceName) {
        super(deviceName + " è offline");
    }
}
