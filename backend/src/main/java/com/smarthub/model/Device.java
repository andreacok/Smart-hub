package com.smarthub.model;

import java.time.Instant;
import java.util.List;

/** Dispositivo normalizzato (modello interno del backend). */
public record Device(
        String id,
        String name,
        String roomId,
        DeviceType type,
        boolean online,
        DeviceState state,
        List<Capability> capabilities,
        Instant updatedAt
) {
    public boolean has(Capability capability) {
        return capabilities.contains(capability);
    }

    public boolean isOn() {
        return state.power() == PowerState.ON;
    }

    public Device withState(DeviceState newState, Instant at) {
        return new Device(id, name, roomId, type, online, newState, capabilities, at);
    }

    public Device withOnline(boolean value) {
        return new Device(id, name, roomId, type, value, state, capabilities, updatedAt);
    }
}
