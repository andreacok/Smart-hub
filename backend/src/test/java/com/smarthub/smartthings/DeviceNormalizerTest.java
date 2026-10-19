package com.smarthub.smartthings;

import com.smarthub.model.Capability;
import com.smarthub.model.ContactState;
import com.smarthub.model.Device;
import com.smarthub.model.DeviceFamily;
import com.smarthub.model.DeviceType;
import com.smarthub.model.OpenState;
import com.smarthub.model.OperatingState;
import com.smarthub.model.PowerState;
import com.smarthub.smartthings.api.StAttribute;
import com.smarthub.smartthings.api.StCapabilityRef;
import com.smarthub.smartthings.api.StCategory;
import com.smarthub.smartthings.api.StComponent;
import com.smarthub.smartthings.api.StDevice;
import com.smarthub.smartthings.api.StDeviceStatus;
import com.smarthub.smartthings.mapping.DeviceNormalizer;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeviceNormalizerTest {

    private final DeviceNormalizer normalizer = new DeviceNormalizer(Map.of());
    private final Instant now = Instant.parse("2026-01-01T10:00:00Z");

    @Test
    void dimmableColorLightIsNormalized() {
        StDevice raw = device("d1", "Light", "switch", "switchLevel", "colorControl", "colorTemperature");
        StDeviceStatus status = status(Map.of(
                "switch", Map.of("switch", attr("on")),
                "switchLevel", Map.of("level", attr(70)),
                "colorControl", Map.of("hue", attr(50.0), "saturation", attr(80.0)),
                "colorTemperature", Map.of("colorTemperature", attr(2700))));

        Device d = normalizer.normalize(raw, status, true, now);

        assertEquals(DeviceType.LIGHT, d.deviceType());
        assertEquals(PowerState.ON, d.state().power());
        assertEquals(70, d.state().brightness());
        assertEquals(180, d.state().color().hue());
        assertEquals(80, d.state().color().saturation());
        assertEquals(2700, d.state().colorTemperature());
        assertTrue(d.capabilities().containsAll(List.of(Capability.POWER, Capability.DIMMER, Capability.COLOR,
                Capability.COLOR_TEMPERATURE)));
    }

    @Test
    void shutterUsesShadeLevelAsPosition() {
        StDevice raw = device("s1", "Blind", "windowShade", "windowShadeLevel");
        StDeviceStatus status = status(Map.of(
                "windowShade", Map.of("windowShade", attr("partially open")),
                "windowShadeLevel", Map.of("shadeLevel", attr(60))));

        Device d = normalizer.normalize(raw, status, true, now);

        assertEquals(DeviceType.BLIND, d.deviceType());
        assertEquals(60, d.state().position());
        assertEquals(OpenState.PARTIALLY_OPEN, d.state().openState());
        assertNull(d.state().brightness());
        assertTrue(d.capabilities().containsAll(List.of(Capability.OPEN_CLOSE, Capability.POSITION, Capability.STOP)));
    }

    @Test
    void shutterWithSwitchLevelOnlyIsDetectedByCategory() {
        StDevice raw = device("s2", "Blind", "switchLevel");
        StDeviceStatus status = status(Map.of("switchLevel", Map.of("level", attr(0))));

        Device d = normalizer.normalize(raw, status, true, now);

        assertEquals(DeviceType.BLIND, d.deviceType());
        assertEquals(0, d.state().position());
        assertEquals(OpenState.CLOSED, d.state().openState());
        assertTrue(d.has(Capability.POSITION));
        assertFalse(d.has(Capability.DIMMER));
    }

    @Test
    void thermostatReadsSetpointModeAndOperatingState() {
        StDevice raw = device("t1", "Thermostat", "temperatureMeasurement", "thermostatHeatingSetpoint",
                "thermostatMode", "thermostatOperatingState");
        StDeviceStatus status = status(Map.of(
                "temperatureMeasurement", Map.of("temperature", new StAttribute(20.5, "C")),
                "thermostatHeatingSetpoint", Map.of("heatingSetpoint", attr(21.5)),
                "thermostatMode", Map.of("thermostatMode", attr("heat"),
                        "supportedThermostatModes", attr(List.of("off", "heat"))),
                "thermostatOperatingState", Map.of("thermostatOperatingState", attr("heating"))));

        Device d = normalizer.normalize(raw, status, true, now);

        assertEquals(DeviceType.THERMOSTAT, d.deviceType());
        assertEquals(20.5, d.state().temperature());
        assertEquals("C", d.state().temperatureUnit());
        assertEquals(21.5, d.state().targetTemperature());
        assertEquals("heat", d.state().thermostatMode());
        assertEquals(List.of("off", "heat"), d.state().supportedThermostatModes());
        assertEquals(OperatingState.HEATING, d.state().operatingState());
    }

    @Test
    void sensorsAndFallbackByCapabilities() {
        StDevice raw = device("x1", null, "contactSensor", "battery");
        StDeviceStatus status = status(Map.of(
                "contactSensor", Map.of("contact", attr("open")),
                "battery", Map.of("battery", attr(12))));

        Device d = normalizer.normalize(raw, status, false, now);

        // Nessuna categoria: la contact + battery non bastano a un tipo specifico, quindi sensore generico.
        assertEquals(DeviceType.GENERIC_SENSOR, d.deviceType());
        assertEquals(DeviceFamily.SENSOR, d.family());
        assertEquals(ContactState.OPEN, d.state().contact());
        assertEquals(12, d.state().battery());
        assertFalse(d.online());
    }

    @Test
    void plainSwitchWithoutCategoryIsSwitchAndOverrideWins() {
        StDevice raw = device("p1", null, "switch");
        assertEquals(DeviceType.SWITCH, normalizer.resolveType(raw));

        DeviceNormalizer withOverride = new DeviceNormalizer(Map.of("p1", DeviceType.LIGHT));
        assertEquals(DeviceType.LIGHT, withOverride.resolveType(raw));
    }

    // -------------------------------------------------------------- helpers

    static StDevice device(String id, String category, String... capabilities) {
        List<StCapabilityRef> caps = Arrays.stream(capabilities).map(c -> new StCapabilityRef(c, 1)).toList();
        List<StCategory> categories = category == null ? List.of() : List.of(new StCategory(category, "manufacturer"));
        return new StDevice(id, "name-" + id, "Label " + id, "loc", "room",
                List.of(new StComponent("main", caps, categories)));
    }

    static StDeviceStatus status(Map<String, Map<String, StAttribute>> main) {
        return new StDeviceStatus(Map.of("main", new LinkedHashMap<>(main)));
    }

    static StAttribute attr(Object value) {
        return new StAttribute(value, null);
    }
}
