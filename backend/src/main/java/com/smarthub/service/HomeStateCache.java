package com.smarthub.service;

import com.smarthub.config.SmartThingsProperties;
import com.smarthub.model.Device;
import com.smarthub.model.DeviceState;
import com.smarthub.model.Location;
import com.smarthub.model.Room;
import com.smarthub.model.Scene;
import com.smarthub.smartthings.api.SmartThingsApi;
import com.smarthub.smartthings.api.SmartThingsException;
import com.smarthub.error.ApiErrorCode;
import com.smarthub.smartthings.error.SmartThingsErrorMapper;
import com.smarthub.smartthings.error.SmartThingsOperation;
import com.smarthub.smartthings.api.StDevice;
import com.smarthub.smartthings.api.StDeviceStatus;
import com.smarthub.smartthings.api.StLocation;
import com.smarthub.smartthings.mapping.DeviceNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Snapshot in memoria della casa (location, stanze, dispositivi normalizzati, scene).
 * <p>
 * Il frontend legge sempre da qui, quindi le sue richieste frequenti (polling) non generano
 * traffico verso SmartThings: è il backend a rinfrescare lo stato con cadenza controllata.
 * Ogni variazione di stato viene pubblicata come {@link DeviceStateChangedEvent}, punto di aggancio
 * per future notifiche push (SSE / WebSocket) o per i webhook SmartThings.
 */
@Service
public class HomeStateCache {

    private static final Logger log = LoggerFactory.getLogger(HomeStateCache.class);

    private final SmartThingsApi api;
    private final DeviceNormalizer normalizer;
    private final SmartThingsProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;
    private final ExecutorService pool;

    private final Map<String, StDevice> rawDevices = new ConcurrentHashMap<>();
    private final Map<String, Device> devices = new ConcurrentHashMap<>();
    private final Map<String, Boolean> health = new ConcurrentHashMap<>();
    private final Object structureLock = new Object();
    private final Object statusLock = new Object();

    private volatile Location location;
    private volatile List<Room> rooms = List.of();
    private volatile List<Scene> scenes = List.of();
    private volatile List<String> order = List.of();

    private volatile Instant lastStructureRefresh;
    private volatile Instant lastStatusRefresh;
    private volatile Instant lastHealthRefresh;
    private volatile String lastError;
    private volatile Instant lastErrorAt;

    @Autowired
    public HomeStateCache(SmartThingsApi api, DeviceNormalizer normalizer, SmartThingsProperties properties,
                          ApplicationEventPublisher events) {
        this(api, normalizer, properties, events, Clock.systemUTC());
    }

    HomeStateCache(SmartThingsApi api, DeviceNormalizer normalizer, SmartThingsProperties properties,
                   ApplicationEventPublisher events, Clock clock) {
        this.api = api;
        this.normalizer = normalizer;
        this.properties = properties;
        this.events = events;
        this.clock = clock;
        AtomicInteger counter = new AtomicInteger();
        this.pool = Executors.newFixedThreadPool(Math.max(1, properties.getStatusConcurrency()), r -> {
            Thread t = new Thread(r, "st-status-" + counter.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
    }

    // ------------------------------------------------------------- lettura

    /** Garantisce che i dati siano stati caricati almeno una volta (primo accesso dopo l'avvio). */
    public void ensureLoaded() {
        if (lastStructureRefresh != null) {
            return;
        }
        synchronized (structureLock) {
            if (lastStructureRefresh == null) {
                refreshStructure();
                refreshStatuses(true);
            }
        }
    }

    public boolean isLoaded() {
        return lastStructureRefresh != null;
    }

    public Location location() {
        ensureLoaded();
        return location;
    }

    public List<Room> rooms() {
        ensureLoaded();
        return rooms;
    }

    public List<Scene> scenes() {
        ensureLoaded();
        return scenes;
    }

    /** Dispositivi nell'ordine restituito da SmartThings. */
    public List<Device> devices() {
        ensureLoaded();
        List<Device> result = new ArrayList<>(order.size());
        for (String id : order) {
            Device d = devices.get(id);
            if (d != null) {
                result.add(d);
            }
        }
        return result;
    }

    public Optional<Device> device(String id) {
        ensureLoaded();
        return Optional.ofNullable(devices.get(id));
    }

    public Optional<StDevice> rawDevice(String id) {
        ensureLoaded();
        return Optional.ofNullable(rawDevices.get(id));
    }

    public Instant lastStructureRefresh() {
        return lastStructureRefresh;
    }

    public Instant lastStatusRefresh() {
        return lastStatusRefresh;
    }

    public String lastError() {
        return lastError;
    }

    public Instant lastErrorAt() {
        return lastErrorAt;
    }

    // ------------------------------------------------------------ refresh

    /** Rilegge location, stanze, elenco dispositivi e scene. */
    public void refreshStructure() {
        synchronized (structureLock) {
            try {
                List<StLocation> locations = api.listLocations();
                StLocation selected = selectLocation(locations);
                List<Room> newRooms = api.listRooms(selected.locationId()).stream()
                        .map(r -> new Room(r.roomId(), r.name()))
                        .toList();
                List<StDevice> raw = api.listDevices(selected.locationId());
                List<Scene> newScenes = loadScenes(selected.locationId());

                List<String> newOrder = new ArrayList<>();
                for (StDevice d : raw) {
                    newOrder.add(d.deviceId());
                    rawDevices.put(d.deviceId(), d);
                    devices.compute(d.deviceId(), (id, existing) -> {
                        // aggiorna definizione (nome, stanza) mantenendo lo stato già noto
                        Device fresh = normalizer.normalize(d, null, health.getOrDefault(id, true), now());
                        return existing == null ? fresh : new Device(fresh.id(), fresh.name(), fresh.roomId(),
                                fresh.smartThingsCategory(), fresh.deviceType(), existing.online(), existing.state(),
                                fresh.capabilities(), existing.updatedAt());
                    });
                }
                rawDevices.keySet().retainAll(newOrder);
                devices.keySet().retainAll(newOrder);
                health.keySet().retainAll(newOrder);

                this.location = new Location(selected.locationId(), selected.name());
                this.rooms = newRooms;
                this.scenes = newScenes;
                this.order = List.copyOf(newOrder);
                this.lastStructureRefresh = now();
                clearError();
                log.info("Struttura caricata: location '{}', {} stanze, {} dispositivi, {} scene",
                        selected.name(), newRooms.size(), newOrder.size(), newScenes.size());
            } catch (RuntimeException e) {
                recordError(e instanceof SmartThingsException st ? st.getMessage()
                        : "Errore interno durante l'aggiornamento da SmartThings");
                throw e;
            }
        }
    }

    /** Rilegge lo stato di tutti i dispositivi (e, se dovuto, lo stato di salute). */
    public void refreshStatuses(boolean forceHealth) {
        synchronized (statusLock) {
            List<String> ids = order;
            boolean withHealth = forceHealth || lastHealthRefresh == null
                    || lastHealthRefresh.plus(properties.getHealthRefreshInterval()).isBefore(now());
            AtomicInteger failures = new AtomicInteger();
            List<CompletableFuture<Void>> futures = new ArrayList<>();
            for (String id : ids) {
                futures.add(CompletableFuture.runAsync(() -> {
                    try {
                        refreshDeviceInternal(id, withHealth);
                    } catch (RuntimeException e) {
                        failures.incrementAndGet();
                        log.debug("Refresh stato fallito per {}: {}", id, e.getMessage());
                    }
                }, pool));
            }
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            if (withHealth) {
                lastHealthRefresh = now();
            }
            if (!ids.isEmpty() && failures.get() == ids.size()) {
                recordError("Impossibile aggiornare lo stato dei dispositivi");
            } else {
                if (failures.get() > 0) {
                    log.warn("Refresh stato: {} dispositivi su {} non aggiornati", failures.get(), ids.size());
                }
                lastStatusRefresh = now();
                clearError();
            }
        }
    }

    /** Rilegge lo stato di un singolo dispositivo (es. dopo un comando). */
    public Optional<Device> refreshDevice(String id) {
        try {
            refreshDeviceInternal(id, false);
        } catch (RuntimeException e) {
            log.debug("Refresh del dispositivo {} fallito: {}", id, e.getMessage());
        }
        return Optional.ofNullable(devices.get(id));
    }

    /** Applica uno stato atteso (aggiornamento ottimistico lato backend). */
    public Optional<Device> applyPredictedState(String id, DeviceState predicted) {
        Device updated = devices.computeIfPresent(id, (k, d) -> d.withState(predicted, now()));
        if (updated != null) {
            events.publishEvent(new DeviceStateChangedEvent(updated));
        }
        return Optional.ofNullable(updated);
    }

    private void refreshDeviceInternal(String id, boolean withHealth) {
        StDevice raw = rawDevices.get(id);
        if (raw == null) {
            return;
        }
        if (withHealth) {
            try {
                health.put(id, api.getDeviceHealth(id).isOnline());
            } catch (RuntimeException e) {
                log.debug("Health non disponibile per {}: {}", id, e.getMessage());
            }
        }
        StDeviceStatus status = api.getDeviceStatus(id);
        Device fresh = normalizer.normalize(raw, status, health.getOrDefault(id, true), now());
        Device previous = devices.put(id, fresh);
        if (previous == null || !Objects.equals(previous.state(), fresh.state()) || previous.online() != fresh.online()) {
            events.publishEvent(new DeviceStateChangedEvent(fresh));
        }
    }

    private StLocation selectLocation(List<StLocation> locations) {
        if (locations.isEmpty()) {
            throw locationError("Nessuna location trovata sull'account SmartThings");
        }
        String configured = properties.getLocationId();
        if (configured != null && !configured.isBlank()) {
            return locations.stream().filter(l -> configured.equals(l.locationId())).findFirst()
                    .orElseThrow(() -> locationError("SMARTTHINGS_LOCATION_ID non trovato tra le location dell'account"));
        }
        if (locations.size() > 1) {
            log.info("Trovate {} location, uso '{}'. Imposta SMARTTHINGS_LOCATION_ID per sceglierne un'altra.",
                    locations.size(), locations.get(0).name());
        }
        return locations.get(0);
    }

    private List<Scene> loadScenes(String locationId) {
        try {
            return api.listScenes(locationId).stream()
                    .map(s -> new Scene(s.sceneId(), s.sceneName(), s.sceneIcon()))
                    .toList();
        } catch (RuntimeException e) {
            // Le scene richiedono scope dedicati (r:scenes:*): non sono indispensabili.
            log.warn("Scene non disponibili: {}", e.getMessage());
            return List.of();
        }
    }

    private void recordError(String message) {
        lastError = message;
        lastErrorAt = now();
        log.warn("Errore SmartThings: {}", message);
    }

    private static SmartThingsException locationError(String message) {
        return new SmartThingsException(SmartThingsErrorMapper.custom(SmartThingsOperation.READ,
                ApiErrorCode.SMARTTHINGS_ERROR, message, message));
    }

    private void clearError() {
        lastError = null;
        lastErrorAt = null;
    }

    private Instant now() {
        return clock.instant();
    }
}
