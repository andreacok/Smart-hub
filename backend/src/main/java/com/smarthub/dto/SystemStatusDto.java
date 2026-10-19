package com.smarthub.dto;

import java.time.Instant;

/** Stato della connessione con SmartThings: utile per verificare la configurazione live. */
public record SystemStatusDto(
        String mode,
        String auth,
        boolean connected,
        String locationName,
        int roomCount,
        int deviceCount,
        Instant lastStructureRefresh,
        Instant lastStatusRefresh,
        String lastError,
        Instant lastErrorAt
) {
}
