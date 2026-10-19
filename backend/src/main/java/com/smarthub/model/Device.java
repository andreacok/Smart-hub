package com.smarthub.model;

import java.time.Instant;
import java.util.List;

/**
 * Dispositivo normalizzato (modello interno del backend).
 *
 * @param smartThingsCategory categoria originale SmartThings, preservata così com'è; null se assente
 * @param deviceType          tipo normalizzato Smart Hub
 */
public record Device(
        String id,
        String name,
        String roomId,
        String smartThingsCategory,
        DeviceType deviceType,
        boolean online,
        DeviceState state,
        List<Capability> capabilities,
        Instant updatedAt
) {
    public DeviceFamily family() {
        return deviceType.family();
    }

    public boolean has(Capability capability) {
        return capabilities.contains(capability);
    }

    public boolean isOn() {
        return state.power() == PowerState.ON;
    }

    public Device withState(DeviceState newState, Instant at) {
        return new Device(id, name, roomId, smartThingsCategory, deviceType, online, newState, capabilities, at);
    }

    public Device withOnline(boolean value) {
        return new Device(id, name, roomId, smartThingsCategory, deviceType, value, state, capabilities, updatedAt);
    }
}
