package com.smarthub.dto;

import com.smarthub.model.Capability;
import com.smarthub.model.Device;
import com.smarthub.model.DeviceFamily;
import com.smarthub.model.DeviceState;
import com.smarthub.model.DeviceType;

import java.time.Instant;
import java.util.List;

/**
 * Dispositivo normalizzato esposto al frontend.
 * {@code smartThingsCategory} è la categoria originale SmartThings; {@code deviceType} e {@code deviceFamily}
 * sono la classificazione Smart Hub.
 */
public record DeviceDto(
        String id,
        String name,
        String roomId,
        String roomName,
        String smartThingsCategory,
        DeviceType deviceType,
        DeviceFamily deviceFamily,
        boolean online,
        DeviceState state,
        List<Capability> capabilities,
        Instant updatedAt
) {
    public static DeviceDto from(Device device, String roomName) {
        return new DeviceDto(device.id(), device.name(), device.roomId(), roomName, device.smartThingsCategory(),
                device.deviceType(), device.family(), device.online(), device.state(), device.capabilities(),
                device.updatedAt());
    }

    public DeviceDto withRoomId(String newRoomId) {
        return new DeviceDto(id, name, newRoomId, roomName, smartThingsCategory, deviceType, deviceFamily, online,
                state, capabilities, updatedAt);
    }
}
