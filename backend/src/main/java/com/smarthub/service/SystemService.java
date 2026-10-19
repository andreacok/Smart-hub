package com.smarthub.service;

import com.smarthub.config.SmartThingsProperties;
import com.smarthub.dto.SystemStatusDto;
import com.smarthub.smartthings.live.TokenProvider;
import org.springframework.stereotype.Service;

/** Informazioni diagnostiche sulla connessione SmartThings. */
@Service
public class SystemService {

    private final HomeStateCache cache;
    private final SmartThingsProperties properties;
    private final TokenProvider tokenProvider;

    public SystemService(HomeStateCache cache, SmartThingsProperties properties, TokenProvider tokenProvider) {
        this.cache = cache;
        this.properties = properties;
        this.tokenProvider = tokenProvider;
    }

    public SystemStatusDto status() {
        boolean loaded = false;
        String error = null;
        try {
            cache.ensureLoaded();
            loaded = true;
        } catch (RuntimeException e) {
            error = e.getMessage();
        }
        boolean connected = loaded && cache.lastError() == null;
        return new SystemStatusDto(
                properties.getMode().name().toLowerCase(),
                properties.isMock() ? "nessuna (dati simulati)" : tokenProvider.describe(),
                connected,
                loaded && cache.location() != null ? cache.location().name() : null,
                loaded ? cache.rooms().size() : 0,
                loaded ? cache.devices().size() : 0,
                cache.lastStructureRefresh(),
                cache.lastStatusRefresh(),
                error != null ? error : cache.lastError(),
                cache.lastErrorAt()
        );
    }
}
