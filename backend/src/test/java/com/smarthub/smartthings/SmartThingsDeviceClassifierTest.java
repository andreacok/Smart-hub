package com.smarthub.smartthings;

import com.smarthub.model.DeviceFamily;
import com.smarthub.model.DeviceType;
import com.smarthub.smartthings.api.StCapabilityRef;
import com.smarthub.smartthings.api.StCategory;
import com.smarthub.smartthings.api.StComponent;
import com.smarthub.smartthings.api.StDevice;
import com.smarthub.smartthings.mapping.SmartThingsDeviceClassifier;
import com.smarthub.smartthings.mapping.SmartThingsDeviceClassifier.Classification;
import com.smarthub.smartthings.mapping.SmartThingsDeviceClassifier.Source;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class SmartThingsDeviceClassifierTest {

    private final SmartThingsDeviceClassifier classifier = new SmartThingsDeviceClassifier(Map.of());

    // --- casi della specifica: categoria del main + temperatureMeasurement non deve mai diventare termostato

    @ParameterizedTest(name = "{0} + temperatureMeasurement -> {1} ({2})")
    @CsvSource({
            "Refrigerator, REFRIGERATOR, REFRIGERATION",
            "Oven, OVEN, COOKING_APPLIANCE",
            "Microwave, MICROWAVE, COOKING_APPLIANCE",
            "Thermostat, THERMOSTAT, CLIMATE",
            "AirConditioner, AIR_CONDITIONER, CLIMATE",
            "TempSensor, TEMP_SENSOR, SENSOR",
    })
    void categoryWinsOverTemperatureCapability(String category, DeviceType type, DeviceFamily family) {
        Classification c = classifier.classify(device("d", List.of(comp("main", category, "temperatureMeasurement"))));
        assertEquals(type, c.deviceType());
        assertEquals(family, c.family());
        assertEquals(category, c.smartThingsCategory());
        assertEquals(Source.SMARTTHINGS_CATEGORY, c.source());
    }

    @ParameterizedTest(name = "{0} + switch -> {1} ({2})")
    @CsvSource({
            "Light, LIGHT, LIGHTING",
            "SmartPlug, SMART_PLUG, POWER",
            "Washer, WASHER, LAUNDRY",
            "Dryer, DRYER, LAUNDRY",
            "Dishwasher, DISHWASHER, CLEANING",
            "RobotCleaner, ROBOT_CLEANER, CLEANING",
            "Television, TELEVISION, MEDIA",
            "Camera, CAMERA, SECURITY",
            "SmartLock, SMART_LOCK, ACCESS",
    })
    void knownCategoriesResolveToTheirType(String category, DeviceType type, DeviceFamily family) {
        Classification c = classifier.classify(device("d", List.of(comp("main", category, "switch"))));
        assertEquals(type, c.deviceType());
        assertEquals(family, c.family());
    }

    @Test
    void categoryMatchIsCaseInsensitiveButOriginalValueIsPreserved() {
        Classification c = classifier.classify(device("d", List.of(comp("main", "refrigerator"))));
        assertEquals(DeviceType.REFRIGERATOR, c.deviceType());
        assertEquals("refrigerator", c.smartThingsCategory());
    }

    // --- scelta della categoria tra component

    @Test
    void specificCategoryBeatsOthersInTheSameComponent() {
        StComponent main = new StComponent("main", caps("switch"),
                List.of(cat("Others", "manufacturer"), cat("Refrigerator", "manufacturer")));
        Classification c = classifier.classify(device("d", List.of(main)));
        assertEquals(DeviceType.REFRIGERATOR, c.deviceType());
        assertEquals("Refrigerator", c.smartThingsCategory());
    }

    @Test
    void userCategoryHasPriorityOverManufacturer() {
        StComponent main = new StComponent("main", caps(),
                List.of(cat("Refrigerator", "manufacturer"), cat("Oven", "user")));
        assertEquals(DeviceType.OVEN, classifier.classify(device("d", List.of(main))).deviceType());
    }

    @Test
    void othersFromMainIsIgnoredWhenMainHasNothingSpecific() {
        Classification c = classifier.classify(device("d", List.of(
                comp("main", "Others"), comp("other", "Refrigerator"))));
        assertEquals(DeviceType.REFRIGERATOR, c.deviceType());
    }

    @Test
    void mainOthersWithSmartPlugOnAnotherComponentResolvesToSmartPlug() {
        // Definito: una categoria specifica di un altro component vince su "Others" del main.
        Classification c = classifier.classify(device("d", List.of(
                comp("main", "Others"), comp("other", "SmartPlug"))));
        assertEquals(DeviceType.SMART_PLUG, c.deviceType());
        assertEquals("SmartPlug", c.smartThingsCategory());
    }

    @Test
    void knownCategoryOnAnotherComponentIsUsedWhenMainHasUnknownName() {
        Classification c = classifier.classify(device("d", List.of(
                comp("main", "SuperFutureSamsungDevice"), comp("cooling", "Refrigerator"))));
        assertEquals(DeviceType.REFRIGERATOR, c.deviceType());
    }

    // --- casi senza categoria specifica

    @Test
    void futureUnknownCategoryDoesNotFailAndKeepsOriginalValue() {
        Classification c = classifier.classify(device("d", List.of(comp("main", "SuperFutureSamsungDevice"))));
        assertEquals(DeviceType.UNKNOWN, c.deviceType());
        assertEquals(DeviceFamily.GENERIC, c.family());
        assertEquals("SuperFutureSamsungDevice", c.smartThingsCategory());
        assertEquals(Source.NONE, c.source());
    }

    @Test
    void genericSensorAndOthersAreUsedOnlyWhenNothingElseIsKnown() {
        assertEquals(DeviceType.GENERIC_SENSOR,
                classifier.classify(device("d", List.of(comp("main", "GenericSensor")))).deviceType());
        assertEquals(DeviceType.OTHER,
                classifier.classify(device("d", List.of(comp("main", "Others")))).deviceType());
    }

    @Test
    void noCategoryAndNoCapabilitiesIsUnknown() {
        Classification c = classifier.classify(device("d", List.of(comp("main", null))));
        assertEquals(DeviceType.UNKNOWN, c.deviceType());
        assertNull(c.smartThingsCategory());
    }

    @Test
    void unknownCategoryFallsBackToCapabilitiesButKeepsTheCategory() {
        Classification c = classifier.classify(device("d", List.of(comp("main", "SuperFuture", "switch"))));
        assertEquals(DeviceType.SWITCH, c.deviceType());
        assertEquals("SuperFuture", c.smartThingsCategory());
        assertEquals(Source.CAPABILITY_FALLBACK, c.source());
    }

    // --- fallback da capabilities: solo combinazioni forti

    @Test
    void temperatureAloneIsNeverAThermostat() {
        Classification c = classifier.classify(device("d", List.of(comp("main", null, "temperatureMeasurement"))));
        assertEquals(DeviceType.GENERIC_SENSOR, c.deviceType());
        assertEquals(DeviceFamily.SENSOR, c.family());
    }

    @Test
    void thermostatModeAloneIsNotEnoughForAThermostat() {
        Classification c = classifier.classify(device("d", List.of(comp("main", null, "thermostatMode"))));
        assertEquals(DeviceType.UNKNOWN, c.deviceType());
    }

    @Test
    void thermostatModeWithSetpointIsAThermostatWhenNoCategory() {
        Classification c = classifier.classify(device("d", List.of(
                comp("main", null, "temperatureMeasurement", "thermostatMode", "thermostatHeatingSetpoint"))));
        assertEquals(DeviceType.THERMOSTAT, c.deviceType());
        assertEquals(Source.CAPABILITY_FALLBACK, c.source());
    }

    @Test
    void weakCategoryYieldsToStrongCapabilityCombination() {
        Classification c = classifier.classify(device("d", List.of(
                comp("main", "Others", "thermostatMode", "thermostatCoolingSetpoint"))));
        assertEquals(DeviceType.THERMOSTAT, c.deviceType());
        assertEquals("Others", c.smartThingsCategory());
    }

    @Test
    void switchWithLevelWithoutCategoryIsALight() {
        Classification c = classifier.classify(device("d", List.of(comp("main", null, "switch", "switchLevel"))));
        assertEquals(DeviceType.LIGHT, c.deviceType());
    }

    // --- override esplicito

    @Test
    void configuredOverrideWinsOverCategory() {
        SmartThingsDeviceClassifier withOverride =
                new SmartThingsDeviceClassifier(Map.of("d", DeviceType.LIGHT));
        Classification c = withOverride.classify(device("d", List.of(comp("main", "Refrigerator"))));
        assertEquals(DeviceType.LIGHT, c.deviceType());
        assertEquals(Source.OVERRIDE, c.source());
        assertEquals("Refrigerator", c.smartThingsCategory());
    }

    // --- helpers

    private static StDevice device(String id, List<StComponent> components) {
        return new StDevice(id, "name-" + id, "Label " + id, "loc", "room", components);
    }

    private static StComponent comp(String id, String category, String... capabilities) {
        List<StCategory> categories = category == null ? List.of() : List.of(cat(category, "manufacturer"));
        return new StComponent(id, caps(capabilities), categories);
    }

    private static StCategory cat(String name, String type) {
        return new StCategory(name, type);
    }

    private static List<StCapabilityRef> caps(String... ids) {
        return Arrays.stream(ids).map(c -> new StCapabilityRef(c, 1)).toList();
    }
}
