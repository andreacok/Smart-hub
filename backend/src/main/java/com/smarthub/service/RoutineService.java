package com.smarthub.service;

import com.smarthub.config.SmartThingsProperties;
import com.smarthub.dto.RoutineDto;
import com.smarthub.exception.NotFoundException;
import com.smarthub.model.Routine;
import com.smarthub.model.RoutineType;
import com.smarthub.smartthings.api.SmartThingsApi;
import com.smarthub.smartthings.mapping.RoutineMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Routine SmartThings (scene manuali e regole/automazioni) per il tab "Routine Gruppi".
 * Le letture non passano dalla cache della casa: ogni apertura del tab interroga SmartThings.
 */
@Service
public class RoutineService {

    private static final Logger log = LoggerFactory.getLogger(RoutineService.class);

    private static final Comparator<Routine> ORDER = Comparator.comparing(Routine::type)
            .thenComparing(Routine::name, String.CASE_INSENSITIVE_ORDER);

    private final HomeStateCache cache;
    private final SmartThingsApi api;
    private final TaskScheduler scheduler;
    private final Duration refreshDelay;

    public RoutineService(HomeStateCache cache, SmartThingsApi api, TaskScheduler scheduler,
                          SmartThingsProperties properties) {
        this.cache = cache;
        this.api = api;
        this.scheduler = scheduler;
        this.refreshDelay = properties.getCommandRefreshDelay();
    }

    public List<RoutineDto> list() {
        return load(locationId()).stream().map(RoutineDto::from).toList();
    }

    public void execute(String routineId) {
        String locationId = locationId();
        Routine routine = load(locationId).stream().filter(r -> r.id().equals(routineId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Routine non trovata: " + routineId));
        log.info("Esecuzione routine {} '{}'", routine.type(), routine.name());
        if (routine.type() == RoutineType.MANUAL_ROUTINE) {
            api.executeScene(routine.id());
        } else {
            api.executeRule(routine.id(), locationId);
        }
        // una routine può toccare molti dispositivi: rileggiamo lo stato come per le scene
        cache.refreshStatuses(false);
        scheduler.schedule(this::refreshQuietly, Instant.now().plus(refreshDelay.isNegative() ? Duration.ZERO : refreshDelay));
    }

    /** Errori SmartThings propagati: il tab deve mostrare l'errore, non una lista vuota. */
    private List<Routine> load(String locationId) {
        List<Routine> routines = new ArrayList<>();
        api.listScenes(locationId).stream().map(RoutineMapper::fromScene).forEach(routines::add);
        api.listRules(locationId).stream().map(RoutineMapper::fromRule).forEach(routines::add);
        routines.sort(ORDER);
        return routines;
    }

    private String locationId() {
        return cache.location().id();
    }

    private void refreshQuietly() {
        try {
            cache.refreshStatuses(false);
        } catch (RuntimeException e) {
            log.debug("Refresh dopo routine fallito: {}", e.getMessage());
        }
    }
}
