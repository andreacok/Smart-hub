package com.smarthub.service;

import com.smarthub.model.Device;
import com.smarthub.smartthings.api.StAttribute;
import com.smarthub.smartthings.api.StCapabilityRef;
import com.smarthub.smartthings.api.StCategory;
import com.smarthub.smartthings.api.StComponent;
import com.smarthub.smartthings.api.StDevice;
import com.smarthub.smartthings.api.StDeviceStatus;
import com.smarthub.smartthings.mapping.DeviceNormalizer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** La temperatura della stanza non deve mai essere presa da un elettrodomestico che misura una temperatura. */
class HomeAggregatorTemperatureTest {

    private final DeviceNormalizer normalizer = new DeviceNormalizer(Map.of());
    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void refrigeratorTemperatureIsIgnoredWhenRoomHasASensor() {
        Device fridge = normalize("fridge", "Refrigerator", 4.0);
        Device sensor = normalize("sensor", "TempSensor", 21.0);

        assertEquals(21.0, HomeAggregator.averageTemperature(List.of(fridge, sensor)));
    }

    @Test
    void onlyAnApplianceTemperatureMeansNoRoomTemperature() {
        Device oven = normalize("oven", "Oven", 180.0);
        assertNull(HomeAggregator.averageTemperature(List.of(oven)));
    }

    @Test
    void thermostatIsUsedWhenThereIsNoDedicatedSensor() {
        Device thermostat = normalize("t", "Thermostat", 20.5);
        assertEquals(20.5, HomeAggregator.averageTemperature(List.of(thermostat)));
    }

    private Device normalize(String id, String category, double temperature) {
        StDevice raw = new StDevice(id, "name-" + id, "Label " + id, "loc", "room",
                List.of(new StComponent("main",
                        List.of(new StCapabilityRef("temperatureMeasurement", 1)),
                        List.of(new StCategory(category, "manufacturer")))));
        StDeviceStatus status = new StDeviceStatus(Map.of("main", Map.of(
                "temperatureMeasurement", Map.of("temperature", new StAttribute(temperature, "C")))));
        return normalizer.normalize(raw, status, true, now);
    }
}
