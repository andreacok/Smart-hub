package com.smarthub.smartthings;

import com.smarthub.model.DeviceFamily;
import com.smarthub.model.DeviceType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Protezione contro categorie dimenticate: l'elenco sotto è quello ufficiale SmartThings.
 * Se una categoria viene tolta dalla tabella di {@link DeviceType}, o se ne compare una nuova
 * senza essere mappata, questo test deve fallire.
 */
class SmartThingsCategoryCompletenessTest {

    static final List<String> OFFICIAL_CATEGORIES = List.of(
            "AirConditioner", "AirPurifier", "AirQualityDetector", "AirpurifyHumidifier", "BathroomHeater",
            "Battery", "Bed", "Bidet", "Blind", "BloodGlucoseMonitor", "BloodPressureMonitor", "BluetoothTracker",
            "Bridges", "Button", "Camera", "CatToilet", "Charger", "ChlorineSensor", "ClothingCareMachine",
            "CoffeeMaker", "ContactSensor", "Cooktop", "CubeRefrigerator", "CurbPowerMeter", "CyclingSensor",
            "Dehumidifier", "DeliveryRobot", "Dishwasher", "Door", "DoorBell", "Dryer", "Earbuds",
            "ElectricVehicleCharger", "Elevator", "Elliptical", "EnvironmentSensor", "Fan", "Faucet", "Feeder",
            "FitnessMat", "Flashlight", "FlowSensor", "GarageDoor", "GasMeter", "GasValve", "GenericSensor",
            "HealthTracker", "HeatPump", "Heatedmattresspad", "HeatingCoolingMat", "HomeTheater", "Hub", "Humidifier",
            "HumiditySensor", "IndoorCycle", "IrRemote", "Irrigation", "KimchiRefrigerator", "KitchenHood",
            "LeakSensor", "Light", "LightSensor", "MedicalThermometer", "MicroFiberFilter", "Microwave", "Mobile",
            "MobilePresence", "MotionSensor", "MultiFunctionalSensor", "MusicSystem", "NetworkAudio", "Networking",
            "Others", "Oven", "PanicButton", "PetWaterDispenser", "PhSensor", "Pillow", "PlantGrower",
            "PowerMeasurementSensor", "PresenceSensor", "Printer", "PrinterMultiFunction", "Projector", "Pump",
            "RainSensor", "Range", "Receiver", "RemoteController", "Refrigerator", "RiceCooker", "RobotCleaner",
            "Rower", "Safe", "ScaleToMeasureMassOfHumanBody", "Scanner", "SecurityPanel", "SetTop", "Shade", "Shoes",
            "ShoesCareMachine", "Shower", "Siren", "SmartLock", "SmartMonitor", "SmartPlug", "SmokeDetector",
            "SolarPanel", "SoundBar", "SoundMachine", "SoundSensor", "Speaker", "StairClimber", "StepMachine",
            "StickVacuumCleaner", "Storage", "Stove", "Switch", "TagReader", "Television", "TempHumiditySensor",
            "TempSensor", "Thermostat", "Toilet", "TowelRack", "Tracker", "Treadmill", "UPnPMediaRenderer", "Vent",
            "VisionSensor", "VoiceAssistance", "Washer", "WaterFreezeDetector", "WaterHeater", "WaterPurifier",
            "WaterValve", "WeatherStation", "WiFiRouter", "Window", "WindowOpener", "WineCellar", "ZoneSensor");

    @Test
    void enumCoversExactlyTheOfficialCategories() {
        Set<String> mapped = Arrays.stream(DeviceType.values())
                .map(DeviceType::smartThingsCategory)
                .filter(c -> c != null)
                .collect(Collectors.toSet());
        Set<String> official = new HashSet<>(OFFICIAL_CATEGORIES);

        Set<String> missing = new HashSet<>(official);
        missing.removeAll(mapped);
        Set<String> extra = new HashSet<>(mapped);
        extra.removeAll(official);

        assertTrue(missing.isEmpty(), "Categorie ufficiali non mappate: " + missing);
        assertTrue(extra.isEmpty(), "Categorie mappate ma non ufficiali: " + extra);
        assertEquals(OFFICIAL_CATEGORIES.size(), official.size(), "L'elenco di riferimento non deve avere duplicati");
    }

    @ParameterizedTest
    @ValueSource(strings = {"AirConditioner", "Refrigerator", "Oven", "Light", "SmartPlug", "Thermostat",
            "TempSensor", "Others", "GenericSensor", "Camera", "Blind", "Washer", "Television"})
    void everyCategoryResolvesToAKnownTypeNeverUnknown(String category) {
        DeviceType type = DeviceType.fromSmartThingsCategory(category).orElseThrow(
                () -> new AssertionError("Categoria non riconosciuta: " + category));
        assertFalse(type == DeviceType.UNKNOWN, category);
    }

    @Test
    void everyOfficialCategoryResolvesAndHasAFamily() {
        for (String category : OFFICIAL_CATEGORIES) {
            DeviceType type = DeviceType.fromSmartThingsCategory(category).orElse(null);
            assertNotNull(type, "Categoria ufficiale finita in UNKNOWN: " + category);
            assertNotNull(type.family(), "Nessuna famiglia per: " + category);
        }
    }

    @Test
    void lookupIsCaseInsensitiveAndUnknownCategoriesGiveEmpty() {
        assertEquals(DeviceType.REFRIGERATOR, DeviceType.fromSmartThingsCategory("refrigerator").orElseThrow());
        assertEquals(DeviceType.UPNP_MEDIA_RENDERER, DeviceType.fromSmartThingsCategory("UPNPMEDIARENDERER").orElseThrow());
        assertTrue(DeviceType.fromSmartThingsCategory("SuperFutureSamsungDevice").isEmpty());
        assertTrue(DeviceType.fromSmartThingsCategory(null).isEmpty());
        assertTrue(DeviceType.fromSmartThingsCategory("  ").isEmpty());
    }
}
