package com.smarthub.dto;

/** Stanza con stato aggregato, usata per le card della home. */
public record RoomSummaryDto(
        String id,
        String name,
        int deviceCount,
        int onlineCount,
        int activeCount,
        int lightsOn,
        int lightsTotal,
        Double temperature,
        Double humidity,
        Integer shutterPosition,
        int shutterCount,
        int openContacts,
        boolean motion,
        int alertCount,
        String imageUrl,
        boolean customImage
) {

    /** Copia con la fotografia personalizzata risolta (URL versionato). */
    public RoomSummaryDto withCustomImage(String url) {
        return new RoomSummaryDto(id, name, deviceCount, onlineCount, activeCount, lightsOn, lightsTotal,
                temperature, humidity, shutterPosition, shutterCount, openContacts, motion, alertCount,
                url, true);
    }
}
