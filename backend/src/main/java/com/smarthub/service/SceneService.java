package com.smarthub.service;

import com.smarthub.config.SmartThingsProperties;
import com.smarthub.dto.SceneDto;
import com.smarthub.exception.NotFoundException;
import com.smarthub.smartthings.api.SmartThingsApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Scene SmartThings (elenco ed esecuzione). */
@Service
public class SceneService {

    private static final Logger log = LoggerFactory.getLogger(SceneService.class);

    private final HomeStateCache cache;
    private final SmartThingsApi api;
    private final TaskScheduler scheduler;
    private final Duration refreshDelay;

    public SceneService(HomeStateCache cache, SmartThingsApi api, TaskScheduler scheduler,
                        SmartThingsProperties properties) {
        this.cache = cache;
        this.api = api;
        this.scheduler = scheduler;
        this.refreshDelay = properties.getCommandRefreshDelay();
    }

    public List<SceneDto> list() {
        return cache.scenes().stream().map(SceneDto::from).toList();
    }

    public void execute(String sceneId) {
        SceneDto scene = list().stream().filter(s -> s.id().equals(sceneId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Scena non trovata: " + sceneId));
        log.info("Esecuzione scena '{}'", scene.name());
        api.executeScene(sceneId);
        // una scena tocca molti dispositivi: rileggiamo tutto lo stato
        cache.refreshStatuses(false);
        scheduler.schedule(() -> refreshQuietly(), Instant.now().plus(refreshDelay.isNegative() ? Duration.ZERO : refreshDelay));
    }

    private void refreshQuietly() {
        try {
            cache.refreshStatuses(false);
        } catch (RuntimeException e) {
            log.debug("Refresh dopo scena fallito: {}", e.getMessage());
        }
    }
}
