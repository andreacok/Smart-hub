package com.smarthub.smartthings.api;

import java.util.List;

/** Singolo comando del body di POST /devices/{deviceId}/commands. */
public record StCommand(String component, String capability, String command, List<Object> arguments) {

    public static StCommand of(String capability, String command, Object... args) {
        return new StCommand(StDevice.MAIN, capability, command, List.of(args));
    }
}
