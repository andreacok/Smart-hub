package com.smarthub.dto;

import com.smarthub.model.Device;
import com.smarthub.model.DeviceState;

import java.time.Instant;

/** Solo lo stato corrente di un dispositivo (GET /api/devices/{id}/status). */
public record DeviceStatusDto(String id, boolean online, DeviceState state, Instant updatedAt) {

    public static DeviceStatusDto from(Device device) {
        return new DeviceStatusDto(device.id(), device.online(), device.state(), device.updatedAt());
    }
}
