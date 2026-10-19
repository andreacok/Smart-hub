package com.smarthub.service;

import com.smarthub.config.SmartThingsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Polling periodico verso SmartThings.
 * <ul>
 *     <li>stato dei dispositivi ogni {@code smartthings.status-refresh-interval} (default 5s);</li>
 *     <li>struttura (stanze, dispositivi, scene) ogni {@code smartthings.structure-refresh-interval} (default 5 min).</li>
 * </ul>
 * In futuro potrà essere affiancato/sostituito dai webhook SmartThings (subscription agli eventi).
 */
@Component
public class StateRefreshScheduler {

    private static final Logger log = LoggerFactory.getLogger(StateRefreshScheduler.class);

    private final HomeStateCache cache;
    private final TaskScheduler scheduler;
    private final SmartThingsProperties properties;

    public StateRefreshScheduler(HomeStateCache cache, TaskScheduler scheduler, SmartThingsProperties properties) {
        this.cache = cache;
        this.scheduler = scheduler;
        this.properties = properties;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void start() {
        Duration status = atLeast(properties.getStatusRefreshInterval(), Duration.ofSeconds(1));
        Duration structure = atLeast(properties.getStructureRefreshInterval(), Duration.ofSeconds(30));
        scheduler.scheduleWithFixedDelay(this::refreshStatus, Instant.now().plusSeconds(1), status);
        scheduler.scheduleWithFixedDelay(this::refreshStructure, Instant.now().plus(structure), structure);
        log.info("Polling SmartThings attivo: stato ogni {}s, struttura ogni {}s",
                status.toSeconds(), structure.toSeconds());
    }

    void refreshStatus() {
        try {
            if (!cache.isLoaded()) {
                cache.ensureLoaded();
            } else {
                cache.refreshStatuses(false);
            }
        } catch (RuntimeException e) {
            log.warn("Aggiornamento stato fallito: {}", e.getMessage());
        }
    }

    void refreshStructure() {
        try {
            cache.refreshStructure();
        } catch (RuntimeException e) {
            log.warn("Aggiornamento struttura fallito: {}", e.getMessage());
        }
    }

    private static Duration atLeast(Duration value, Duration min) {
        return value == null || value.compareTo(min) < 0 ? min : value;
    }
}
