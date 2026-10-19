package com.smarthub.smartthings;

import com.smarthub.MutableClock;
import com.smarthub.smartthings.api.SmartThingsException;
import com.smarthub.smartthings.api.StCommand;
import com.smarthub.smartthings.api.StDevice;
import com.smarthub.smartthings.mock.MockSmartThingsApi;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MockSmartThingsApiTest {

    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T10:00:00Z"));
    private final MockSmartThingsApi api = new MockSmartThingsApi(Duration.ZERO, 0.0, clock);

    @Test
    void demoHomeContainsRequiredRoomsAndDevices() {
        String location = api.listLocations().get(0).locationId();
        List<String> rooms = api.listRooms(location).stream().map(r -> r.name()).toList();
        assertTrue(rooms.containsAll(List.of("Salotto", "Camera da Letto", "Cucina", "Corridoio")));

        List<StDevice> devices = api.listDevices(location);
        assertTrue(devices.size() >= 10);
        assertTrue(devices.stream().anyMatch(d -> d.displayName().equals("Lampadario")));
        assertFalse(api.listScenes(location).isEmpty());
    }

    @Test
    void switchCommandChangesStatus() {
        api.executeCommands("salotto-piantana", List.of(StCommand.of("switch", "on")));
        assertEquals("on", api.getDeviceStatus("salotto-piantana").attribute("switch", "switch").orElseThrow().value());
    }

    @Test
    void shutterMovesGraduallyToTarget() {
        String id = "camera-da-letto-tapparella"; // parte chiusa (0%)
        api.executeCommands(id, List.of(StCommand.of("windowShade", "open")));

        clock.advance(Duration.ofSeconds(2));
        Object midway = api.getDeviceStatus(id).attribute("windowShadeLevel", "shadeLevel").orElseThrow().value();
        assertEquals("opening", api.getDeviceStatus(id).attribute("windowShade", "windowShade").orElseThrow().value());
        assertTrue(((Number) midway).intValue() > 0 && ((Number) midway).intValue() < 100);

        clock.advance(Duration.ofSeconds(30));
        assertEquals(100, api.getDeviceStatus(id).attribute("windowShadeLevel", "shadeLevel").orElseThrow().value());
        assertEquals("open", api.getDeviceStatus(id).attribute("windowShade", "windowShade").orElseThrow().value());
    }

    @Test
    void offlineDeviceRejectsCommands() {
        assertFalse(api.getDeviceHealth("corridoio-videocamera").isOnline());
        assertThrows(SmartThingsException.class,
                () -> api.executeCommands("corridoio-videocamera", List.of(StCommand.of("switch", "off"))));
    }

    @Test
    void sceneAppliesCommands() {
        api.executeScene("scene-buonanotte");
        assertEquals("off", api.getDeviceStatus("salotto-lampadario").attribute("switch", "switch").orElseThrow().value());
    }
}
