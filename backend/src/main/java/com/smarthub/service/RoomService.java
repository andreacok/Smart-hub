package com.smarthub.service;

import com.smarthub.dto.ActionResultDto;
import com.smarthub.dto.AlertDto;
import com.smarthub.dto.CommandRequest;
import com.smarthub.dto.DeviceDto;
import com.smarthub.dto.RoomDetailDto;
import com.smarthub.dto.RoomSummaryDto;
import com.smarthub.exception.NotFoundException;
import com.smarthub.model.CommandType;
import com.smarthub.model.Device;
import com.smarthub.model.Room;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/** Stanze: riepiloghi aggregati, dettaglio e azioni di stanza. */
@Service
public class RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomService.class);

    private final HomeStateCache cache;
    private final DeviceService deviceService;
    private final RoomImageService roomImageService;

    public RoomService(HomeStateCache cache, DeviceService deviceService, RoomImageService roomImageService) {
        this.cache = cache;
        this.deviceService = deviceService;
        this.roomImageService = roomImageService;
    }

    public List<RoomSummaryDto> list() {
        List<Device> devices = cache.devices();
        List<AlertDto> alerts = alertsWithEffectiveRooms(devices);
        Map<Room, List<Device>> grouped = groupByRoom(devices);
        return grouped.entrySet().stream()
                .map(e -> roomImageService.decorate(HomeAggregator.roomSummary(e.getKey(), e.getValue(), alerts)))
                .toList();
    }

    public RoomDetailDto get(String roomId) {
        List<Device> devices = cache.devices();
        Map<Room, List<Device>> grouped = groupByRoom(devices);
        Room room = grouped.keySet().stream().filter(r -> r.id().equals(roomId)).findFirst()
                .orElseThrow(() -> new NotFoundException("Stanza non trovata: " + roomId));
        List<AlertDto> alerts = alertsWithEffectiveRooms(devices).stream()
                .filter(a -> roomId.equals(a.roomId())).toList();
        Map<String, String> roomNames = deviceService.roomNames();
        List<DeviceDto> dtos = grouped.get(room).stream().map(d -> deviceService.toDto(d, roomNames)).toList();
        return new RoomDetailDto(roomImageService.decorate(HomeAggregator.roomSummary(room, grouped.get(room), alerts)), dtos, alerts);
    }

    /** Spegne tutte le luci e le prese accese della stanza. */
    public ActionResultDto turnOffAll(String roomId) {
        get(roomId); // verifica esistenza
        Map<String, String> roomNames = deviceService.roomNames();
        return turnOff(d -> roomId.equals(DeviceService.effectiveRoomId(d, roomNames)) && HomeAggregator.isActive(d));
    }

    /** Spegne tutti i dispositivi che soddisfano il filtro, raccogliendo eventuali errori. */
    ActionResultDto turnOff(Predicate<Device> filter) {
        int ok = 0;
        List<String> errors = new ArrayList<>();
        for (Device d : cache.devices()) {
            if (!filter.test(d)) {
                continue;
            }
            try {
                deviceService.execute(d.id(), new CommandRequest(CommandType.TURN_OFF, null));
                ok++;
            } catch (RuntimeException e) {
                log.warn("Spegnimento di '{}' fallito: {}", d.name(), e.getMessage());
                errors.add(d.name() + ": " + e.getMessage());
            }
        }
        return new ActionResultDto(ok, errors.size(), errors);
    }

    /** Raggruppa per stanza mantenendo l'ordine SmartThings; aggiunge la stanza virtuale se necessario. */
    Map<Room, List<Device>> groupByRoom(List<Device> devices) {
        Map<String, String> roomNames = deviceService.roomNames();
        Map<Room, List<Device>> grouped = new LinkedHashMap<>();
        for (Room room : cache.rooms()) {
            grouped.put(room, new ArrayList<>());
        }
        Room unassigned = new Room(HomeAggregator.UNASSIGNED_ROOM_ID, HomeAggregator.UNASSIGNED_ROOM_NAME);
        for (Device d : devices) {
            String roomId = DeviceService.effectiveRoomId(d, roomNames);
            Room key = grouped.keySet().stream().filter(r -> r.id().equals(roomId)).findFirst().orElse(unassigned);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(d);
        }
        return grouped;
    }

    List<AlertDto> alertsWithEffectiveRooms(List<Device> devices) {
        Map<String, String> roomNames = deviceService.roomNames();
        return HomeAggregator.alerts(devices).stream()
                .map(a -> new AlertDto(a.id(), a.severity(), a.kind(), a.title(), a.deviceId(),
                        a.roomId() != null && roomNames.containsKey(a.roomId()) ? a.roomId()
                                : HomeAggregator.UNASSIGNED_ROOM_ID))
                .toList();
    }
}
