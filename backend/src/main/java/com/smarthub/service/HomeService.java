package com.smarthub.service;

import com.smarthub.config.AppProperties;
import com.smarthub.config.SmartThingsProperties;
import com.smarthub.dto.ActionResultDto;
import com.smarthub.dto.AlertDto;
import com.smarthub.dto.DeviceDto;
import com.smarthub.dto.HomeDto;
import com.smarthub.dto.LocationDto;
import com.smarthub.dto.SceneDto;
import com.smarthub.model.Device;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Vista d'insieme della casa per la home dashboard. */
@Service
public class HomeService {

    private final HomeStateCache cache;
    private final RoomService roomService;
    private final DeviceService deviceService;
    private final AppProperties appProperties;
    private final SmartThingsProperties smartThingsProperties;

    public HomeService(HomeStateCache cache, RoomService roomService, DeviceService deviceService,
                       AppProperties appProperties, SmartThingsProperties smartThingsProperties) {
        this.cache = cache;
        this.roomService = roomService;
        this.deviceService = deviceService;
        this.appProperties = appProperties;
        this.smartThingsProperties = smartThingsProperties;
    }

    public HomeDto home() {
        List<Device> devices = cache.devices();
        List<AlertDto> alerts = roomService.alertsWithEffectiveRooms(devices);
        Map<String, String> roomNames = deviceService.roomNames();
        List<DeviceDto> active = devices.stream()
                .filter(HomeAggregator::isActive)
                .map(d -> deviceService.toDto(d, roomNames))
                .toList();
        Instant updatedAt = cache.lastStatusRefresh() != null ? cache.lastStatusRefresh() : Instant.now();
        return new HomeDto(
                LocationDto.from(cache.location()),
                appProperties.getOwnerName(),
                smartThingsProperties.getMode().name().toLowerCase(),
                HomeAggregator.homeSummary(devices, alerts),
                roomService.list(),
                alerts,
                cache.scenes().stream().map(SceneDto::from).toList(),
                active,
                updatedAt
        );
    }

    /** Spegne tutte le luci della casa. */
    public ActionResultDto allLightsOff() {
        return roomService.turnOff(d -> d.deviceType().isLight() && HomeAggregator.isActive(d));
    }
}
