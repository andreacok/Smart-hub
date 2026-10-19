package com.smarthub.dto;

/** Avviso mostrato in dashboard (dispositivo offline, batteria scarica, porta aperta...). */
public record AlertDto(
        String id,
        Severity severity,
        Kind kind,
        String title,
        String deviceId,
        String roomId
) {
    public enum Severity { INFO, WARNING, CRITICAL }

    public enum Kind { OFFLINE, LOW_BATTERY, OPEN_CONTACT }
}
