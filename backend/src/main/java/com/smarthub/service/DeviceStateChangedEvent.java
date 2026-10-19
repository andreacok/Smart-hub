package com.smarthub.service;

import com.smarthub.model.Device;

/**
 * Pubblicato quando lo stato di un dispositivo cambia.
 * Oggi non ha listener (il frontend usa il polling); è il punto di aggancio previsto
 * per Server-Sent Events / WebSocket o per gli eventi ricevuti da webhook SmartThings.
 */
public record DeviceStateChangedEvent(Device device) {
}
