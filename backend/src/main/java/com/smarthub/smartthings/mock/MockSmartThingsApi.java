package com.smarthub.smartthings.mock;

import com.smarthub.config.SmartThingsProperties;
import com.smarthub.smartthings.api.SmartThingsApi;
import com.smarthub.smartthings.api.SmartThingsException;
import com.smarthub.smartthings.api.StCommand;
import com.smarthub.smartthings.api.StDevice;
import com.smarthub.smartthings.api.StDeviceStatus;
import com.smarthub.smartthings.api.StHealth;
import com.smarthub.smartthings.api.StLocation;
import com.smarthub.smartthings.api.StRoom;
import com.smarthub.smartthings.api.StScene;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;

import static com.smarthub.smartthings.api.StCapabilities.*;

/**
 * Implementazione simulata delle API SmartThings: una casa demo realistica tenuta in memoria.
 * Restituisce dati nello stesso formato grezzo delle API reali, così normalizzazione,
 * cache e controller vengono esercitati esattamente come in modalità live.
 */
public class MockSmartThingsApi implements SmartThingsApi {

    private static final Logger log = LoggerFactory.getLogger(MockSmartThingsApi.class);

    static final String LOCATION_ID = "mock-location";

    private final Duration latency;
    private final double commandFailureRate;
    private final Clock clock;

    private final List<StRoom> rooms = new ArrayList<>();
    private final Map<String, MockDevice> devices = new LinkedHashMap<>();
    private final Map<String, MockScene> scenes = new LinkedHashMap<>();

    private record MockScene(StScene scene, Function<MockDevice, List<StCommand>> commands) {
    }

    public MockSmartThingsApi(SmartThingsProperties.Mock config) {
        this(config.getLatency(), config.getCommandFailureRate(), Clock.systemUTC());
    }

    public MockSmartThingsApi(Duration latency, double commandFailureRate, Clock clock) {
        this.latency = latency == null ? Duration.ZERO : latency;
        this.commandFailureRate = commandFailureRate;
        this.clock = clock;
        buildHome();
        log.info("SmartThings in modalità MOCK: casa demo con {} stanze e {} dispositivi", rooms.size(), devices.size());
    }

    // ---------------------------------------------------------------- API

    @Override
    public List<StLocation> listLocations() {
        simulateLatency();
        return List.of(new StLocation(LOCATION_ID, "Casa Demo"));
    }

    @Override
    public List<StRoom> listRooms(String locationId) {
        simulateLatency();
        return List.copyOf(rooms);
    }

    @Override
    public synchronized List<StDevice> listDevices(String locationId) {
        simulateLatency();
        return devices.values().stream().map(MockDevice::definition).toList();
    }

    @Override
    public synchronized StDeviceStatus getDeviceStatus(String deviceId) {
        return device(deviceId).snapshot(clock.instant());
    }

    @Override
    public synchronized StHealth getDeviceHealth(String deviceId) {
        return new StHealth(deviceId, device(deviceId).online() ? "ONLINE" : "OFFLINE");
    }

    @Override
    public void executeCommands(String deviceId, List<StCommand> commands) {
        simulateLatency();
        synchronized (this) {
            MockDevice device = device(deviceId);
            if (!device.online()) {
                throw new SmartThingsException("Il dispositivo è offline", 409, null);
            }
            if (commandFailureRate > 0 && ThreadLocalRandom.current().nextDouble() < commandFailureRate) {
                throw new SmartThingsException("Errore simulato durante l'invio del comando", 503, null);
            }
            commands.forEach(c -> device.apply(c, clock.instant()));
        }
    }

    @Override
    public List<StScene> listScenes(String locationId) {
        simulateLatency();
        return scenes.values().stream().map(MockScene::scene).toList();
    }

    @Override
    public void executeScene(String sceneId) {
        simulateLatency();
        MockScene scene = scenes.get(sceneId);
        if (scene == null) {
            throw new SmartThingsException("Scena non trovata: " + sceneId, 404, null);
        }
        synchronized (this) {
            for (MockDevice device : devices.values()) {
                if (device.online()) {
                    scene.commands().apply(device).forEach(c -> device.apply(c, clock.instant()));
                }
            }
        }
    }

    private MockDevice device(String deviceId) {
        MockDevice d = devices.get(deviceId);
        if (d == null) {
            throw new SmartThingsException("Dispositivo non trovato: " + deviceId, 404, null);
        }
        return d;
    }

    private void simulateLatency() {
        if (latency.isZero() || latency.isNegative()) {
            return;
        }
        try {
            Thread.sleep(latency.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ----------------------------------------------------------- casa demo

    /**
     * Stanze ufficiali di SmartHub (Salotto, Camera da Letto, Cameretta, Cucina, Bagno Grande, Bagno Piccolo,
     * Corridoio) più Ripostiglio, stanza di test con nome non ufficiale: deve ricevere la foto di default.
     */
    private void buildHome() {
        room("salotto", "Salotto");
        room("camera-da-letto", "Camera da Letto");
        room("cameretta", "Cameretta");
        room("cucina", "Cucina");
        room("bagno-grande", "Bagno Grande");
        room("bagno-piccolo", "Bagno Piccolo");
        room("corridoio", "Corridoio");
        room("ripostiglio", "Ripostiglio");

        // Salotto
        light("salotto-lampadario", "Lampadario", "salotto", true, 75).set(COLOR_TEMPERATURE, "colorTemperature", 3000, "K");
        light("salotto-piantana", "Piantana", "salotto", false, 50);
        light("salotto-led-tv", "LED TV", "salotto", true, 40)
                .set(COLOR_CONTROL, "hue", 66.0).set(COLOR_CONTROL, "saturation", 80.0);
        shutter("salotto-tapparella-finestra", "Tapparella finestra", "salotto", 80);
        shutter("salotto-tapparella-balcone", "Tapparella balcone", "salotto", 35);
        add(new MockDevice("salotto-porta-finestra", "Porta finestra balcone", "salotto", LOCATION_ID, "ContactSensor")
                .set(CONTACT, "contact", "open")
                .set(BATTERY, "battery", 91, "%"));
        add(new MockDevice("salotto-termostato", "Termostato", "salotto", LOCATION_ID, "Thermostat")
                .simulatedTemperature(21.2, 0.0)
                .set(THERMOSTAT_HEATING_SETPOINT, "heatingSetpoint", 21.5, "C")
                .set(THERMOSTAT_MODE, "thermostatMode", "heat")
                .set(THERMOSTAT_MODE, "supportedThermostatModes", List.of("off", "heat", "eco", "auto"))
                .set(THERMOSTAT_OPERATING_STATE, "thermostatOperatingState", "idle"));
        add(new MockDevice("salotto-sensore-clima", "Sensore clima", "salotto", LOCATION_ID, "MultiFunctionalSensor")
                .simulatedTemperature(22.4, 0.7)
                .set(HUMIDITY, "humidity", 48, "%")
                .set(BATTERY, "battery", 86, "%"));

        // Camera da Letto
        light("camera-da-letto-luce", "Luce principale", "camera-da-letto", false, 100);
        light("camera-da-letto-comodino", "Lampada comodino", "camera-da-letto", true, 30)
                .set(COLOR_TEMPERATURE, "colorTemperature", 2700, "K");
        shutter("camera-da-letto-tapparella", "Tapparella", "camera-da-letto", 0);
        add(new MockDevice("camera-da-letto-sensore", "Sensore temperatura", "camera-da-letto", LOCATION_ID, "TempSensor")
                .simulatedTemperature(20.1, 1.9)
                .set(BATTERY, "battery", 14, "%"));

        // Cameretta
        light("cameretta-luce", "Luce principale", "cameretta", false, 100);
        light("cameretta-scrivania", "Luce scrivania", "cameretta", true, 90)
                .set(COLOR_TEMPERATURE, "colorTemperature", 4500, "K");
        shutter("cameretta-tapparella", "Tapparella", "cameretta", 50);
        add(new MockDevice("cameretta-sensore", "Sensore temperatura", "cameretta", LOCATION_ID, "TempSensor")
                .simulatedTemperature(21.0, 0.0)
                .set(BATTERY, "battery", 81, "%"));

        // Cucina
        add(new MockDevice("cucina-luce", "Luce principale", "cucina", LOCATION_ID, "Light")
                .set(SWITCH, "switch", "on"));
        add(new MockDevice("cucina-piano", "Luce piano cucina", "cucina", LOCATION_ID, "Light")
                .set(SWITCH, "switch", "on"));
        add(new MockDevice("cucina-presa-caffe", "Macchina caffè", "cucina", LOCATION_ID, "SmartPlug")
                .set(SWITCH, "switch", "off")
                .set(POWER_METER, "nominal", 1150.0, "W")
                .set(POWER_METER, "power", 0.0, "W"));

        // Bagno Grande
        add(new MockDevice("bagno-grande-luce", "Luce principale", "bagno-grande", LOCATION_ID, "Light")
                .set(SWITCH, "switch", "off"));
        add(new MockDevice("bagno-grande-specchio", "Luce specchio", "bagno-grande", LOCATION_ID, "Light")
                .set(SWITCH, "switch", "off"));
        add(new MockDevice("bagno-grande-sensore", "Sensore temperatura e umidità", "bagno-grande", LOCATION_ID, "MultiFunctionalSensor")
                .simulatedTemperature(23.0, 3.1)
                .set(HUMIDITY, "humidity", 64, "%")
                .set(BATTERY, "battery", 72, "%"));

        // Bagno Piccolo
        add(new MockDevice("bagno-piccolo-luce", "Luce", "bagno-piccolo", LOCATION_ID, "Light")
                .set(SWITCH, "switch", "off"));
        add(new MockDevice("bagno-piccolo-sensore", "Sensore temperatura e umidità", "bagno-piccolo", LOCATION_ID, "MultiFunctionalSensor")
                .simulatedTemperature(22.6, 0.4)
                .set(HUMIDITY, "humidity", 58, "%")
                .set(BATTERY, "battery", 94, "%"));

        // Corridoio
        light("corridoio-luce", "Luce corridoio", "corridoio", true, 60);
        add(new MockDevice("corridoio-movimento", "Sensore movimento", "corridoio", LOCATION_ID, "MotionSensor")
                .simulatedMotion()
                .set(BATTERY, "battery", 67, "%"));
        add(new MockDevice("corridoio-videocamera", "Videocamera", "corridoio", LOCATION_ID, "Camera")
                .set(SWITCH, "switch", "on")
                .set(VIDEO_CAMERA, "camera", "on")
                .offline());

        // Ripostiglio: stanza di test, nome non ufficiale
        light("ripostiglio-luce", "Luce ripostiglio", "ripostiglio", false, 100);

        // Scene
        scene("scene-buongiorno", "Buongiorno", "sunrise", d -> {
            if (d.has(WINDOW_SHADE)) {
                return List.of(StCommand.of(WINDOW_SHADE, "open"));
            }
            if (d.id().equals("cucina-presa-caffe")) {
                return List.of(StCommand.of(SWITCH, "on"));
            }
            return List.of();
        });
        scene("scene-film", "Film", "movie", d -> switch (d.id()) {
            case "salotto-lampadario" -> List.of(StCommand.of(SWITCH_LEVEL, "setLevel", 15));
            case "salotto-piantana" -> List.of(StCommand.of(SWITCH, "off"));
            case "salotto-led-tv" -> List.of(StCommand.of(SWITCH, "on"), StCommand.of(SWITCH_LEVEL, "setLevel", 35));
            case "salotto-tapparella-finestra", "salotto-tapparella-balcone" ->
                    List.of(StCommand.of(WINDOW_SHADE_LEVEL, "setShadeLevel", 10));
            default -> List.of();
        });
        scene("scene-buonanotte", "Buonanotte", "moon", d -> {
            if (d.has(WINDOW_SHADE)) {
                return List.of(StCommand.of(WINDOW_SHADE, "close"));
            }
            if (d.has(SWITCH) && !d.has(POWER_METER) && !d.has(VIDEO_CAMERA)) {
                return List.of(StCommand.of(SWITCH, "off"));
            }
            return List.of();
        });
        scene("scene-esco", "Esco di casa", "door", d -> {
            if (d.has(THERMOSTAT_MODE)) {
                return List.of(StCommand.of(THERMOSTAT_MODE, "setThermostatMode", "eco"));
            }
            if (d.has(SWITCH) && !d.has(VIDEO_CAMERA)) {
                return List.of(StCommand.of(SWITCH, "off"));
            }
            return List.of();
        });
    }

    private void room(String id, String name) {
        rooms.add(new StRoom(id, LOCATION_ID, name));
    }

    private MockDevice light(String id, String label, String roomId, boolean on, int level) {
        return add(new MockDevice(id, label, roomId, LOCATION_ID, "Light")
                .set(SWITCH, "switch", on ? "on" : "off")
                .set(SWITCH_LEVEL, "level", level, "%"));
    }

    private MockDevice shutter(String id, String label, String roomId, int position) {
        return add(new MockDevice(id, label, roomId, LOCATION_ID, "Blind")
                .set(WINDOW_SHADE, "windowShade", MockDevice.shadeState(position))
                .set(WINDOW_SHADE, "supportedWindowShadeCommands", List.of("open", "close", "pause"))
                .set(WINDOW_SHADE_LEVEL, "shadeLevel", position, "%"));
    }

    private MockDevice add(MockDevice device) {
        devices.put(device.id(), device);
        return device;
    }

    private void scene(String id, String name, String icon, Function<MockDevice, List<StCommand>> commands) {
        scenes.put(id, new MockScene(new StScene(id, name, icon, LOCATION_ID), commands));
    }
}
