package com.smarthub.service;

import com.smarthub.MutableClock;
import com.smarthub.config.SmartThingsProperties;
import com.smarthub.dto.AlertDto;
import com.smarthub.dto.HomeSummaryDto;
import com.smarthub.dto.RoomSummaryDto;
import com.smarthub.model.Device;
import com.smarthub.model.DeviceType;
import com.smarthub.model.PowerState;
import com.smarthub.model.Room;
import com.smarthub.smartthings.mapping.DeviceNormalizer;
import com.smarthub.smartthings.mock.MockSmartThingsApi;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HomeStateCacheTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
    private final List<Object> events = new ArrayList<>();
    private final HomeStateCache cache = new HomeStateCache(
            new MockSmartThingsApi(Duration.ZERO, 0.0, clock),
            new DeviceNormalizer(Map.of()),
            new SmartThingsProperties(),
            events::add,
            clock);

    @Test
    void loadsNormalizedHome() {
        assertEquals("Casa Demo", cache.location().name());
        assertEquals(8, cache.rooms().size());
        Device lampadario = cache.device("salotto-lampadario").orElseThrow();
        assertEquals(DeviceType.LIGHT, lampadario.type());
        assertEquals(PowerState.ON, lampadario.state().power());
        assertEquals(75, lampadario.state().brightness());
        assertFalse(cache.device("corridoio-videocamera").orElseThrow().online());
        assertFalse(events.isEmpty());
    }

    @Test
    void aggregatesRoomsAndAlerts() {
        List<Device> devices = cache.devices();
        List<AlertDto> alerts = HomeAggregator.alerts(devices);
        assertTrue(alerts.stream().anyMatch(a -> a.kind() == AlertDto.Kind.OFFLINE));
        assertTrue(alerts.stream().anyMatch(a -> a.kind() == AlertDto.Kind.LOW_BATTERY));
        assertTrue(alerts.stream().anyMatch(a -> a.kind() == AlertDto.Kind.OPEN_CONTACT));

        Room soggiorno = cache.rooms().stream().filter(r -> r.id().equals("salotto")).findFirst().orElseThrow();
        RoomSummaryDto summary = HomeAggregator.roomSummary(soggiorno,
                devices.stream().filter(d -> "salotto".equals(d.roomId())).toList(), alerts);
        assertEquals(2, summary.lightsOn());
        assertEquals(3, summary.lightsTotal());
        assertEquals(2, summary.shutterCount());
        assertNotNull(summary.temperature());

        HomeSummaryDto home = HomeAggregator.homeSummary(devices, alerts);
        assertEquals(devices.size(), home.devicesTotal());
        assertEquals(alerts.size(), home.alertCount());
        assertNotNull(home.powerUsage());
    }
}
