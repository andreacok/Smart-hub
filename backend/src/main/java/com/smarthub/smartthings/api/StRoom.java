package com.smarthub.smartthings.api;

/** Room così come restituita da GET /locations/{locationId}/rooms. */
public record StRoom(String roomId, String locationId, String name) {
}
