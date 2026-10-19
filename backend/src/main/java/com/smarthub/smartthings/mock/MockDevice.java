package com.smarthub.smartthings.mock;

import com.smarthub.smartthings.api.SmartThingsException;
import com.smarthub.smartthings.error.SmartThingsErrorMapper;
import com.smarthub.smartthings.error.SmartThingsOperation;
import com.smarthub.smartthings.api.StAttribute;
import com.smarthub.smartthings.api.StCapabilityRef;
import com.smarthub.smartthings.api.StCategory;
import com.smarthub.smartthings.api.StCommand;
import com.smarthub.smartthings.api.StComponent;
import com.smarthub.smartthings.api.StDevice;
import com.smarthub.smartthings.api.StDeviceStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.smarthub.smartthings.api.StCapabilities.*;

/**
 * Dispositivo simulato che si comporta come un dispositivo SmartThings:
 * espone definizione e status nel formato grezzo e reagisce ai comandi SmartThings.
 * Le tapparelle si muovono gradualmente e le temperature oscillano leggermente nel tempo.
 */
class MockDevice {

    private static final double SHUTTER_SPEED_PER_SECOND = 12.0;

    private final String id;
    private final String label;
    private final String roomId;
    private final String locationId;
    private final String category;
    private final Map<String, Map<String, StAttribute>> status = new LinkedHashMap<>();
    private boolean online = true;

    // simulazione movimento tapparella
    private double moveFrom;
    private double moveTarget;
    private Instant moveStart;

    // simulazione ambiente
    private double baseTemperature = Double.NaN;
    private double phase;
    private boolean simulateMotion;

    MockDevice(String id, String label, String roomId, String locationId, String category) {
        this.id = id;
        this.label = label;
        this.roomId = roomId;
        this.locationId = locationId;
        this.category = category;
    }

    String id() {
        return id;
    }

    String roomId() {
        return roomId;
    }

    boolean online() {
        return online;
    }

    MockDevice offline() {
        this.online = false;
        return this;
    }

    MockDevice set(String capability, String attribute, Object value) {
        return set(capability, attribute, value, null);
    }

    MockDevice set(String capability, String attribute, Object value, String unit) {
        status.computeIfAbsent(capability, k -> new LinkedHashMap<>()).put(attribute, new StAttribute(value, unit));
        return this;
    }

    MockDevice simulatedTemperature(double base, double phase) {
        this.baseTemperature = base;
        this.phase = phase;
        return set(TEMPERATURE, "temperature", base, "C");
    }

    MockDevice simulatedMotion() {
        this.simulateMotion = true;
        return set(MOTION, "motion", "inactive");
    }

    boolean has(String capability) {
        return status.containsKey(capability);
    }

    Object value(String capability, String attribute) {
        Map<String, StAttribute> cap = status.get(capability);
        StAttribute a = cap == null ? null : cap.get(attribute);
        return a == null ? null : a.value();
    }

    StDevice definition() {
        List<StCapabilityRef> caps = new ArrayList<>();
        status.keySet().forEach(c -> caps.add(new StCapabilityRef(c, 1)));
        caps.add(new StCapabilityRef("refresh", 1));
        StComponent main = new StComponent(StDevice.MAIN, caps, List.of(new StCategory(category, "manufacturer")));
        return new StDevice(id, label, label, locationId, roomId, List.of(main));
    }

    StDeviceStatus snapshot(Instant now) {
        tick(now);
        Map<String, Map<String, StAttribute>> copy = new LinkedHashMap<>();
        status.forEach((cap, attrs) -> copy.put(cap, new LinkedHashMap<>(attrs)));
        return new StDeviceStatus(Map.of(StDevice.MAIN, copy));
    }

    // ------------------------------------------------------------ commands

    void apply(StCommand command, Instant now) {
        tick(now);
        List<Object> args = command.arguments() == null ? List.of() : command.arguments();
        String key = command.capability() + "." + command.command();
        switch (key) {
            case SWITCH + ".on" -> setPower(true);
            case SWITCH + ".off" -> setPower(false);
            case SWITCH_LEVEL + ".setLevel" -> {
                int level = clamp(intArg(args), 0, 100);
                set(SWITCH_LEVEL, "level", level, "%");
                if (has(SWITCH)) {
                    setPower(level > 0);
                }
            }
            case COLOR_TEMPERATURE + ".setColorTemperature" -> {
                set(COLOR_TEMPERATURE, "colorTemperature", intArg(args), "K");
                setPower(true);
            }
            case COLOR_CONTROL + ".setColor" -> {
                if (!args.isEmpty() && args.get(0) instanceof Map<?, ?> map) {
                    set(COLOR_CONTROL, "hue", toDouble(map.get("hue")));
                    set(COLOR_CONTROL, "saturation", toDouble(map.get("saturation")));
                    setPower(true);
                }
            }
            case WINDOW_SHADE + ".open" -> startMove(100, now);
            case WINDOW_SHADE + ".close" -> startMove(0, now);
            case WINDOW_SHADE + ".pause" -> stopMove();
            case WINDOW_SHADE_LEVEL + ".setShadeLevel" -> startMove(clamp(intArg(args), 0, 100), now);
            case THERMOSTAT_HEATING_SETPOINT + ".setHeatingSetpoint" ->
                    set(THERMOSTAT_HEATING_SETPOINT, "heatingSetpoint", toDouble(args.get(0)), "C");
            case THERMOSTAT_MODE + ".setThermostatMode" ->
                    set(THERMOSTAT_MODE, "thermostatMode", String.valueOf(args.get(0)));
            default -> throw new SmartThingsException(SmartThingsErrorMapper.fromHttp(SmartThingsOperation.COMMAND, id, 422,
                    SmartThingsErrorMapper.simulatedBody("Comando non supportato dal dispositivo simulato: " + key), null));
        }
        tick(now);
    }

    private void setPower(boolean on) {
        if (!has(SWITCH)) {
            return;
        }
        set(SWITCH, "switch", on ? "on" : "off");
    }

    // ---------------------------------------------------------- simulation

    void tick(Instant now) {
        if (moveStart != null) {
            double elapsed = (now.toEpochMilli() - moveStart.toEpochMilli()) / 1000.0;
            double distance = SHUTTER_SPEED_PER_SECOND * elapsed;
            double pos;
            if (moveTarget >= moveFrom) {
                pos = Math.min(moveTarget, moveFrom + distance);
            } else {
                pos = Math.max(moveTarget, moveFrom - distance);
            }
            set(WINDOW_SHADE_LEVEL, "shadeLevel", (int) Math.round(pos), "%");
            if (pos == moveTarget) {
                moveStart = null;
                set(WINDOW_SHADE, "windowShade", shadeState((int) Math.round(pos)));
            } else {
                set(WINDOW_SHADE, "windowShade", moveTarget > moveFrom ? "opening" : "closing");
            }
        }

        if (!Double.isNaN(baseTemperature)) {
            double minutes = now.getEpochSecond() / 60.0;
            double t = baseTemperature + 0.4 * Math.sin(minutes / 9.0 + phase);
            set(TEMPERATURE, "temperature", Math.round(t * 10) / 10.0, "C");
        }

        if (has(THERMOSTAT_OPERATING_STATE)) {
            Object mode = value(THERMOSTAT_MODE, "thermostatMode");
            Object temp = value(TEMPERATURE, "temperature");
            Object setpoint = value(THERMOSTAT_HEATING_SETPOINT, "heatingSetpoint");
            String op = "idle";
            if ("heat".equals(mode) && temp instanceof Number t && setpoint instanceof Number sp
                    && t.doubleValue() < sp.doubleValue() - 0.2) {
                op = "heating";
            }
            set(THERMOSTAT_OPERATING_STATE, "thermostatOperatingState", op);
        }

        if (simulateMotion) {
            // 30 secondi di movimento ogni 3 minuti
            boolean active = (now.getEpochSecond() / 30) % 6 == 0;
            set(MOTION, "motion", active ? "active" : "inactive");
        }

        if (has(POWER_METER)) {
            boolean on = "on".equals(value(SWITCH, "switch"));
            Object nominal = value(POWER_METER, "nominal");
            double watts = on && nominal instanceof Number n ? n.doubleValue() : 0.0;
            set(POWER_METER, "power", watts, "W");
        }
    }

    private void startMove(int target, Instant now) {
        Object current = value(WINDOW_SHADE_LEVEL, "shadeLevel");
        moveFrom = current instanceof Number n ? n.doubleValue() : 0;
        moveTarget = target;
        moveStart = now;
    }

    private void stopMove() {
        moveStart = null;
        Object current = value(WINDOW_SHADE_LEVEL, "shadeLevel");
        int pos = current instanceof Number n ? n.intValue() : 0;
        set(WINDOW_SHADE, "windowShade", shadeState(pos));
    }

    static String shadeState(int position) {
        if (position <= 0) {
            return "closed";
        }
        return position >= 100 ? "open" : "partially open";
    }

    private static int intArg(List<Object> args) {
        if (args.isEmpty()) {
            throw new SmartThingsException(SmartThingsErrorMapper.fromHttp(SmartThingsOperation.COMMAND, null, 422,
                    SmartThingsErrorMapper.simulatedBody("Argomento mancante"), null));
        }
        return (int) Math.round(toDouble(args.get(0)));
    }

    private static double toDouble(Object o) {
        if (o instanceof Number n) {
            return n.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(o));
        } catch (NumberFormatException e) {
            throw new SmartThingsException(SmartThingsErrorMapper.fromHttp(SmartThingsOperation.COMMAND, null, 422,
                    SmartThingsErrorMapper.simulatedBody("Argomento non numerico"), null), e);
        }
    }

    private static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
