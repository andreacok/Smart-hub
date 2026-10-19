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
        int alertCount
) {
}
