package com.smarthub.dto;

import com.smarthub.model.Capability;
import com.smarthub.model.Device;
import com.smarthub.model.DeviceState;
import com.smarthub.model.DeviceType;

import java.time.Instant;
import java.util.List;

/** Dispositivo normalizzato esposto al frontend. */
public record DeviceDto(
        String id,
        String name,
        String roomId,
        String roomName,
        DeviceType type,
        boolean online,
        DeviceState state,
        List<Capability> capabilities,
        Instant updatedAt
) {
    public static DeviceDto from(Device device, String roomName) {
        return new DeviceDto(device.id(), device.name(), device.roomId(), roomName, device.type(), device.online(),
                device.state(), device.capabilities(), device.updatedAt());
    }

    public DeviceDto withRoomId(String newRoomId) {
        return new DeviceDto(id, name, newRoomId, roomName, type, online, state, capabilities, updatedAt);
    }
}
