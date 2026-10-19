package com.smarthub.service;

import com.smarthub.config.SmartThingsProperties;
import com.smarthub.dto.CommandRequest;
import com.smarthub.dto.DeviceDto;
import com.smarthub.dto.DeviceStatusDto;
import com.smarthub.exception.DeviceOfflineException;
import com.smarthub.exception.NotFoundException;
import com.smarthub.model.Device;
import com.smarthub.model.DeviceCommand;
import com.smarthub.model.DeviceState;
import com.smarthub.model.OpenState;
import com.smarthub.model.Room;
import com.smarthub.smartthings.api.SmartThingsApi;
import com.smarthub.smartthings.api.StCommand;
import com.smarthub.smartthings.api.StDevice;
import com.smarthub.smartthings.mapping.CommandTranslator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Lettura dei dispositivi e invio comandi. */
@Service
public class DeviceService {

    private static final Logger log = LoggerFactory.getLogger(DeviceService.class);
    private static final int MAX_MOVEMENT_REFRESHES = 40;

    private final HomeStateCache cache;
    private final SmartThingsApi api;
    private final CommandTranslator translator;
    private final TaskScheduler scheduler;
    private final Duration refreshDelay;

    public DeviceService(HomeStateCache cache, SmartThingsApi api, CommandTranslator translator,
                         TaskScheduler scheduler, SmartThingsProperties properties) {
        this.cache = cache;
        this.api = api;
        this.translator = translator;
        this.scheduler = scheduler;
        this.refreshDelay = properties.getCommandRefreshDelay();
    }

    public List<DeviceDto> list(String roomId) {
        Map<String, String> roomNames = roomNames();
        return cache.devices().stream()
                .filter(d -> roomId == null || roomId.isBlank() || roomId.equals(effectiveRoomId(d, roomNames)))
                .map(d -> toDto(d, roomNames))
                .toList();
    }

    public DeviceDto get(String id) {
        return toDto(find(id), roomNames());
    }

    public DeviceStatusDto status(String id) {
        return DeviceStatusDto.from(find(id));
    }

    /**
     * Esegue un comando normalizzato. La risposta contiene già lo stato atteso, così il frontend
     * può riallinearsi subito; lo stato reale viene riletto poco dopo da SmartThings.
     */
    public DeviceDto execute(String id, CommandRequest request) {
        Device device = find(id);
        if (!device.online()) {
            throw new DeviceOfflineException(device.name());
        }
        StDevice raw = cache.rawDevice(id).orElseThrow(() -> notFound(id));
        DeviceCommand command = new DeviceCommand(request.command(), request.value());
        List<StCommand> stCommands = translator.translate(raw, device.deviceType(), command, device.state());
        log.info("Comando {} su '{}' ({})", command.type(), device.name(), id);

        api.executeCommands(id, stCommands);

        DeviceState predicted = translator.predict(device.state(), command);
        Device updated = cache.applyPredictedState(id, predicted).orElse(device);
        scheduleRefresh(id);
        return toDto(updated, roomNames());
    }

    void scheduleRefresh(String id) {
        if (refreshDelay == null || refreshDelay.isZero() || refreshDelay.isNegative()) {
            cache.refreshDevice(id);
            return;
        }
        scheduler.schedule(() -> refreshWhileMoving(id, MAX_MOVEMENT_REFRESHES), Instant.now().plus(refreshDelay));
    }

    /**
     * Rilegge il dispositivo; se è una tapparella ancora in movimento continua a seguirla
     * più spesso del polling normale, così la posizione in UI avanza in modo fluido.
     */
    private void refreshWhileMoving(String id, int remaining) {
        boolean moving = cache.refreshDevice(id)
                .map(d -> d.state().openState() == OpenState.OPENING || d.state().openState() == OpenState.CLOSING)
                .orElse(false);
        if (moving && remaining > 0) {
            scheduler.schedule(() -> refreshWhileMoving(id, remaining - 1), Instant.now().plus(refreshDelay));
        }
    }

    Device find(String id) {
        return cache.device(id).orElseThrow(() -> notFound(id));
    }

    DeviceDto toDto(Device device, Map<String, String> roomNames) {
        String roomId = effectiveRoomId(device, roomNames);
        return DeviceDto.from(device, roomNames.get(roomId)).withRoomId(roomId);
    }

    Map<String, String> roomNames() {
        Map<String, String> names = cache.rooms().stream()
                .collect(Collectors.toMap(Room::id, Room::name, (a, b) -> a, java.util.LinkedHashMap::new));
        names.put(HomeAggregator.UNASSIGNED_ROOM_ID, HomeAggregator.UNASSIGNED_ROOM_NAME);
        return names;
    }

    /** I dispositivi senza stanza (o con stanza sconosciuta) finiscono nella stanza virtuale "Altri dispositivi". */
    static String effectiveRoomId(Device device, Map<String, String> roomNames) {
        String roomId = device.roomId();
        return roomId != null && roomNames.containsKey(roomId) ? roomId : HomeAggregator.UNASSIGNED_ROOM_ID;
    }

    private static NotFoundException notFound(String id) {
        return new NotFoundException("Dispositivo non trovato: " + id);
    }
}
