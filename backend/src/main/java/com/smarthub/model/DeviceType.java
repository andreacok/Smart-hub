package com.smarthub.model;

import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Tipo normalizzato del dispositivo SmartThings, una costante per ogni categoria ufficiale.
 * Ogni costante conosce la categoria SmartThings che rappresenta e la famiglia UI a cui appartiene.
 * <p>
 * Questa è la UNICA tabella di riferimento per le categorie: aggiungere una categoria SmartThings
 * significa aggiungere una riga qui (e il relativo test di completezza).
 * <p>
 * Generata da una tabella unica; le costanti restano esplicite e leggibili.
 */
public enum DeviceType {
    AIR_CONDITIONER("AirConditioner", DeviceFamily.CLIMATE),
    AIR_PURIFIER("AirPurifier", DeviceFamily.AIR_QUALITY),
    AIR_QUALITY_DETECTOR("AirQualityDetector", DeviceFamily.AIR_QUALITY),
    AIR_PURIFY_HUMIDIFIER("AirpurifyHumidifier", DeviceFamily.AIR_QUALITY),
    BATHROOM_HEATER("BathroomHeater", DeviceFamily.CLIMATE),
    BATTERY("Battery", DeviceFamily.POWER),
    BED("Bed", DeviceFamily.GENERIC),
    BIDET("Bidet", DeviceFamily.WATER),
    BLIND("Blind", DeviceFamily.COVERING),
    BLOOD_GLUCOSE_MONITOR("BloodGlucoseMonitor", DeviceFamily.PERSONAL_HEALTH),
    BLOOD_PRESSURE_MONITOR("BloodPressureMonitor", DeviceFamily.PERSONAL_HEALTH),
    BLUETOOTH_TRACKER("BluetoothTracker", DeviceFamily.MOBILE_TRACKING),
    BRIDGES("Bridges", DeviceFamily.NETWORK),
    BUTTON("Button", DeviceFamily.REMOTE_CONTROL),
    CAMERA("Camera", DeviceFamily.SECURITY),
    CAT_TOILET("CatToilet", DeviceFamily.PET),
    CHARGER("Charger", DeviceFamily.POWER),
    CHLORINE_SENSOR("ChlorineSensor", DeviceFamily.SENSOR),
    CLOTHING_CARE_MACHINE("ClothingCareMachine", DeviceFamily.LAUNDRY),
    COFFEE_MAKER("CoffeeMaker", DeviceFamily.COOKING_APPLIANCE),
    CONTACT_SENSOR("ContactSensor", DeviceFamily.SENSOR),
    COOKTOP("Cooktop", DeviceFamily.COOKING_APPLIANCE),
    CUBE_REFRIGERATOR("CubeRefrigerator", DeviceFamily.REFRIGERATION),
    CURB_POWER_METER("CurbPowerMeter", DeviceFamily.POWER),
    CYCLING_SENSOR("CyclingSensor", DeviceFamily.FITNESS),
    DEHUMIDIFIER("Dehumidifier", DeviceFamily.AIR_QUALITY),
    DELIVERY_ROBOT("DeliveryRobot", DeviceFamily.GENERIC),
    DISHWASHER("Dishwasher", DeviceFamily.CLEANING),
    DOOR("Door", DeviceFamily.ACCESS),
    DOOR_BELL("DoorBell", DeviceFamily.SECURITY),
    DRYER("Dryer", DeviceFamily.LAUNDRY),
    EARBUDS("Earbuds", DeviceFamily.AUDIO),
    ELECTRIC_VEHICLE_CHARGER("ElectricVehicleCharger", DeviceFamily.POWER),
    ELEVATOR("Elevator", DeviceFamily.GENERIC),
    ELLIPTICAL("Elliptical", DeviceFamily.FITNESS),
    ENVIRONMENT_SENSOR("EnvironmentSensor", DeviceFamily.SENSOR),
    FAN("Fan", DeviceFamily.CLIMATE),
    FAUCET("Faucet", DeviceFamily.WATER),
    FEEDER("Feeder", DeviceFamily.PET),
    FITNESS_MAT("FitnessMat", DeviceFamily.FITNESS),
    FLASHLIGHT("Flashlight", DeviceFamily.LIGHTING),
    FLOW_SENSOR("FlowSensor", DeviceFamily.SENSOR),
    GARAGE_DOOR("GarageDoor", DeviceFamily.ACCESS),
    GAS_METER("GasMeter", DeviceFamily.POWER),
    GAS_VALVE("GasValve", DeviceFamily.POWER),
    GENERIC_SENSOR("GenericSensor", DeviceFamily.SENSOR),
    HEALTH_TRACKER("HealthTracker", DeviceFamily.PERSONAL_HEALTH),
    HEAT_PUMP("HeatPump", DeviceFamily.CLIMATE),
    HEATED_MATTRESS_PAD("Heatedmattresspad", DeviceFamily.CLIMATE),
    HEATING_COOLING_MAT("HeatingCoolingMat", DeviceFamily.CLIMATE),
    HOME_THEATER("HomeTheater", DeviceFamily.MEDIA),
    HUB("Hub", DeviceFamily.NETWORK),
    HUMIDIFIER("Humidifier", DeviceFamily.AIR_QUALITY),
    HUMIDITY_SENSOR("HumiditySensor", DeviceFamily.SENSOR),
    INDOOR_CYCLE("IndoorCycle", DeviceFamily.FITNESS),
    IR_REMOTE("IrRemote", DeviceFamily.REMOTE_CONTROL),
    IRRIGATION("Irrigation", DeviceFamily.WATER),
    KIMCHI_REFRIGERATOR("KimchiRefrigerator", DeviceFamily.REFRIGERATION),
    KITCHEN_HOOD("KitchenHood", DeviceFamily.COOKING_APPLIANCE),
    LEAK_SENSOR("LeakSensor", DeviceFamily.SENSOR),
    LIGHT("Light", DeviceFamily.LIGHTING),
    LIGHT_SENSOR("LightSensor", DeviceFamily.SENSOR),
    MEDICAL_THERMOMETER("MedicalThermometer", DeviceFamily.PERSONAL_HEALTH),
    MICRO_FIBER_FILTER("MicroFiberFilter", DeviceFamily.CLEANING),
    MICROWAVE("Microwave", DeviceFamily.COOKING_APPLIANCE),
    MOBILE("Mobile", DeviceFamily.MOBILE_TRACKING),
    MOBILE_PRESENCE("MobilePresence", DeviceFamily.MOBILE_TRACKING),
    MOTION_SENSOR("MotionSensor", DeviceFamily.SENSOR),
    MULTI_FUNCTIONAL_SENSOR("MultiFunctionalSensor", DeviceFamily.SENSOR),
    MUSIC_SYSTEM("MusicSystem", DeviceFamily.AUDIO),
    NETWORK_AUDIO("NetworkAudio", DeviceFamily.AUDIO),
    NETWORKING("Networking", DeviceFamily.NETWORK),
    OTHER("Others", DeviceFamily.GENERIC),
    OVEN("Oven", DeviceFamily.COOKING_APPLIANCE),
    PANIC_BUTTON("PanicButton", DeviceFamily.REMOTE_CONTROL),
    PET_WATER_DISPENSER("PetWaterDispenser", DeviceFamily.PET),
    PH_SENSOR("PhSensor", DeviceFamily.SENSOR),
    PILLOW("Pillow", DeviceFamily.GENERIC),
    PLANT_GROWER("PlantGrower", DeviceFamily.GENERIC),
    POWER_MEASUREMENT_SENSOR("PowerMeasurementSensor", DeviceFamily.POWER),
    PRESENCE_SENSOR("PresenceSensor", DeviceFamily.SENSOR),
    PRINTER("Printer", DeviceFamily.GENERIC),
    PRINTER_MULTI_FUNCTION("PrinterMultiFunction", DeviceFamily.GENERIC),
    PROJECTOR("Projector", DeviceFamily.MEDIA),
    PUMP("Pump", DeviceFamily.WATER),
    RAIN_SENSOR("RainSensor", DeviceFamily.SENSOR),
    RANGE("Range", DeviceFamily.COOKING_APPLIANCE),
    RECEIVER("Receiver", DeviceFamily.MEDIA),
    REMOTE_CONTROLLER("RemoteController", DeviceFamily.REMOTE_CONTROL),
    REFRIGERATOR("Refrigerator", DeviceFamily.REFRIGERATION),
    RICE_COOKER("RiceCooker", DeviceFamily.COOKING_APPLIANCE),
    ROBOT_CLEANER("RobotCleaner", DeviceFamily.CLEANING),
    ROWER("Rower", DeviceFamily.FITNESS),
    SAFE("Safe", DeviceFamily.ACCESS),
    SCALE_TO_MEASURE_MASS_OF_HUMAN_BODY("ScaleToMeasureMassOfHumanBody", DeviceFamily.PERSONAL_HEALTH),
    SCANNER("Scanner", DeviceFamily.GENERIC),
    SECURITY_PANEL("SecurityPanel", DeviceFamily.SECURITY),
    SET_TOP("SetTop", DeviceFamily.MEDIA),
    SHADE("Shade", DeviceFamily.COVERING),
    SHOES("Shoes", DeviceFamily.GENERIC),
    SHOES_CARE_MACHINE("ShoesCareMachine", DeviceFamily.LAUNDRY),
    SHOWER("Shower", DeviceFamily.WATER),
    SIREN("Siren", DeviceFamily.SECURITY),
    SMART_LOCK("SmartLock", DeviceFamily.ACCESS),
    SMART_MONITOR("SmartMonitor", DeviceFamily.MEDIA),
    SMART_PLUG("SmartPlug", DeviceFamily.POWER),
    SMOKE_DETECTOR("SmokeDetector", DeviceFamily.SECURITY),
    SOLAR_PANEL("SolarPanel", DeviceFamily.POWER),
    SOUND_BAR("SoundBar", DeviceFamily.AUDIO),
    SOUND_MACHINE("SoundMachine", DeviceFamily.AUDIO),
    SOUND_SENSOR("SoundSensor", DeviceFamily.SENSOR),
    SPEAKER("Speaker", DeviceFamily.AUDIO),
    STAIR_CLIMBER("StairClimber", DeviceFamily.FITNESS),
    STEP_MACHINE("StepMachine", DeviceFamily.FITNESS),
    STICK_VACUUM_CLEANER("StickVacuumCleaner", DeviceFamily.CLEANING),
    STORAGE("Storage", DeviceFamily.GENERIC),
    STOVE("Stove", DeviceFamily.COOKING_APPLIANCE),
    SWITCH("Switch", DeviceFamily.POWER),
    TAG_READER("TagReader", DeviceFamily.REMOTE_CONTROL),
    TELEVISION("Television", DeviceFamily.MEDIA),
    TEMP_HUMIDITY_SENSOR("TempHumiditySensor", DeviceFamily.SENSOR),
    TEMP_SENSOR("TempSensor", DeviceFamily.SENSOR),
    THERMOSTAT("Thermostat", DeviceFamily.CLIMATE),
    TOILET("Toilet", DeviceFamily.WATER),
    TOWEL_RACK("TowelRack", DeviceFamily.CLIMATE),
    TRACKER("Tracker", DeviceFamily.MOBILE_TRACKING),
    TREADMILL("Treadmill", DeviceFamily.FITNESS),
    UPNP_MEDIA_RENDERER("UPnPMediaRenderer", DeviceFamily.MEDIA),
    VENT("Vent", DeviceFamily.CLIMATE),
    VISION_SENSOR("VisionSensor", DeviceFamily.SENSOR),
    VOICE_ASSISTANCE("VoiceAssistance", DeviceFamily.AUDIO),
    WASHER("Washer", DeviceFamily.LAUNDRY),
    WATER_FREEZE_DETECTOR("WaterFreezeDetector", DeviceFamily.SENSOR),
    WATER_HEATER("WaterHeater", DeviceFamily.WATER),
    WATER_PURIFIER("WaterPurifier", DeviceFamily.WATER),
    WATER_VALVE("WaterValve", DeviceFamily.WATER),
    WEATHER_STATION("WeatherStation", DeviceFamily.SENSOR),
    WIFI_ROUTER("WiFiRouter", DeviceFamily.NETWORK),
    WINDOW("Window", DeviceFamily.ACCESS),
    WINDOW_OPENER("WindowOpener", DeviceFamily.COVERING),
    WINE_CELLAR("WineCellar", DeviceFamily.REFRIGERATION),
    ZONE_SENSOR("ZoneSensor", DeviceFamily.SENSOR),
    /** Nessuna categoria utilizzabile né capability riconoscibili: tipo sconosciuto, mai un errore. */
    UNKNOWN(null, DeviceFamily.GENERIC);

    private final String smartThingsCategory;
    private final DeviceFamily family;

    DeviceType(String smartThingsCategory, DeviceFamily family) {
        this.smartThingsCategory = smartThingsCategory;
        this.family = family;
    }

    /** Nome della categoria SmartThings, esattamente come la definisce SmartThings. Null per UNKNOWN. */
    public String smartThingsCategory() {
        return smartThingsCategory;
    }

    public DeviceFamily family() {
        return family;
    }

    /** Categorie troppo generiche per decidere da sole il tipo: le capabilities possono essere più precise. */
    public boolean isWeakCategory() {
        return this == OTHER || this == GENERIC_SENSOR;
    }

    // Comodità per il comportamento: così i consumatori non confrontano costanti sparse nel codice.

    public boolean isLight() {
        return this == LIGHT;
    }

    public boolean isShutter() {
        return this == BLIND || this == SHADE;
    }

    /** Prese e interruttori: un'accensione/spegnimento semplice. */
    public boolean isSwitch() {
        return this == SWITCH || this == SMART_PLUG;
    }

    public boolean isThermostat() {
        return this == THERMOSTAT;
    }

    public boolean isCamera() {
        return this == CAMERA;
    }

    public boolean isSensor() {
        return family == DeviceFamily.SENSOR;
    }

    private static final Map<String, DeviceType> BY_CATEGORY = Arrays.stream(values())
            .filter(t -> t.smartThingsCategory != null)
            .collect(Collectors.toUnmodifiableMap(t -> normalize(t.smartThingsCategory), Function.identity()));

    /**
     * Trova il tipo per nome di categoria SmartThings, senza distinguere maiuscole e minuscole.
     * Le categorie sconosciute (anche future) danno {@code Optional.empty()}: non è un errore.
     */
    public static Optional<DeviceType> fromSmartThingsCategory(String category) {
        if (category == null || category.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_CATEGORY.get(normalize(category)));
    }

    private static String normalize(String category) {
        return category.trim().toLowerCase(Locale.ROOT);
    }
}
