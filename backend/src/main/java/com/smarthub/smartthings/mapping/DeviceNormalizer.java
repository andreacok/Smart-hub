package com.smarthub.smartthings.mapping;

import com.smarthub.model.Capability;
import com.smarthub.model.ColorValue;
import com.smarthub.model.ContactState;
import com.smarthub.model.Device;
import com.smarthub.model.DeviceState;
import com.smarthub.model.DeviceType;
import com.smarthub.model.OpenState;
import com.smarthub.model.OperatingState;
import com.smarthub.model.PowerState;
import com.smarthub.smartthings.api.StCategory;
import com.smarthub.smartthings.api.StComponent;
import com.smarthub.smartthings.api.StDevice;
import com.smarthub.smartthings.api.StDeviceStatus;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.smarthub.smartthings.api.StCapabilities.*;

/**
 * Converte la definizione + lo status grezzo SmartThings nel modello normalizzato {@link Device}.
 * È una classe senza stato e senza dipendenze da Spring, quindi facilmente testabile.
 */
public class DeviceNormalizer {

    private final Map<String, DeviceType> typeOverrides;

    public DeviceNormalizer(Map<String, DeviceType> typeOverrides) {
        this.typeOverrides = typeOverrides == null ? Map.of() : Map.copyOf(typeOverrides);
    }

    public Device normalize(StDevice raw, StDeviceStatus status, boolean online, Instant now) {
        DeviceType type = resolveType(raw);
        List<Capability> capabilities = resolveCapabilities(raw, type);
        DeviceState state = status == null ? DeviceState.empty() : resolveState(raw, type, status);
        return new Device(raw.deviceId(), raw.displayName(), raw.roomId(), type, online, state, capabilities, now);
    }

    // ---------------------------------------------------------------- type

    public DeviceType resolveType(StDevice raw) {
        DeviceType override = typeOverrides.get(raw.deviceId());
        if (override != null) {
            return override;
        }
        DeviceType fromCategory = typeFromCategories(raw);
        if (fromCategory != null) {
            return fromCategory;
        }
        return typeFromCapabilities(raw);
    }

    private DeviceType typeFromCategories(StDevice raw) {
        List<StCategory> categories = raw.mainComponent().map(StComponent::categories).orElse(List.of());
        if (categories == null) {
            return null;
        }
        // "user" ha la precedenza su "manufacturer": è la categoria scelta dall'utente nell'app.
        List<StCategory> ordered = new ArrayList<>(categories);
        ordered.sort((a, b) -> Boolean.compare(!"user".equalsIgnoreCase(a.categoryType()),
                !"user".equalsIgnoreCase(b.categoryType())));
        for (StCategory category : ordered) {
            DeviceType t = typeFromCategoryName(category.name());
            if (t != null) {
                return t;
            }
        }
        return null;
    }

    static DeviceType typeFromCategoryName(String name) {
        if (name == null) {
            return null;
        }
        String n = name.toLowerCase(Locale.ROOT);
        if (n.contains("light") || n.contains("lamp") || n.contains("bulb")) {
            return DeviceType.LIGHT;
        }
        if (n.contains("blind") || n.contains("shade") || n.contains("curtain") || n.contains("shutter")
                || n.contains("window")) {
            return DeviceType.SHUTTER;
        }
        if (n.contains("thermostat") || n.contains("airconditioner") || n.contains("heater")) {
            return DeviceType.THERMOSTAT;
        }
        if (n.contains("camera")) {
            return DeviceType.CAMERA;
        }
        if (n.contains("sensor") || n.contains("presence") || n.contains("leak")) {
            return DeviceType.SENSOR;
        }
        if (n.contains("switch") || n.contains("plug") || n.contains("outlet")) {
            return DeviceType.SWITCH;
        }
        return null;
    }

    private DeviceType typeFromCapabilities(StDevice raw) {
        if (raw.hasCapability(VIDEO_CAMERA) || raw.hasCapability(VIDEO_STREAM)) {
            return DeviceType.CAMERA;
        }
        if (raw.hasCapability(WINDOW_SHADE) || raw.hasCapability(WINDOW_SHADE_LEVEL)) {
            return DeviceType.SHUTTER;
        }
        if (raw.hasCapability(THERMOSTAT_MODE) || raw.hasCapability(THERMOSTAT_HEATING_SETPOINT)
                || raw.hasCapability(THERMOSTAT_COOLING_SETPOINT)) {
            return DeviceType.THERMOSTAT;
        }
        if (raw.hasCapability(SWITCH) && (raw.hasCapability(SWITCH_LEVEL) || raw.hasCapability(COLOR_CONTROL)
                || raw.hasCapability(COLOR_TEMPERATURE))) {
            return DeviceType.LIGHT;
        }
        if (raw.hasCapability(SWITCH)) {
            return DeviceType.SWITCH;
        }
        if (raw.hasCapability(MOTION) || raw.hasCapability(CONTACT) || raw.hasCapability(TEMPERATURE)
                || raw.hasCapability(HUMIDITY)) {
            return DeviceType.SENSOR;
        }
        return DeviceType.OTHER;
    }

    // -------------------------------------------------------- capabilities

    public List<Capability> resolveCapabilities(StDevice raw, DeviceType type) {
        Set<Capability> caps = EnumSet.noneOf(Capability.class);
        if (raw.hasCapability(SWITCH)) {
            caps.add(Capability.POWER);
        }
        if (raw.hasCapability(SWITCH_LEVEL)) {
            // Su una tapparella switchLevel rappresenta la posizione, non la luminosità.
            caps.add(type == DeviceType.SHUTTER ? Capability.POSITION : Capability.DIMMER);
        }
        if (raw.hasCapability(COLOR_TEMPERATURE)) {
            caps.add(Capability.COLOR_TEMPERATURE);
        }
        if (raw.hasCapability(COLOR_CONTROL)) {
            caps.add(Capability.COLOR);
        }
        if (raw.hasCapability(WINDOW_SHADE)) {
            caps.add(Capability.OPEN_CLOSE);
            caps.add(Capability.STOP);
        }
        if (raw.hasCapability(WINDOW_SHADE_LEVEL)) {
            caps.add(Capability.POSITION);
        }
        if (raw.hasCapability(TEMPERATURE)) {
            caps.add(Capability.TEMPERATURE);
        }
        if (raw.hasCapability(HUMIDITY)) {
            caps.add(Capability.HUMIDITY);
        }
        if (raw.hasCapability(THERMOSTAT_MODE) || raw.hasCapability(THERMOSTAT_HEATING_SETPOINT)
                || raw.hasCapability(THERMOSTAT_COOLING_SETPOINT)) {
            caps.add(Capability.THERMOSTAT);
        }
        if (raw.hasCapability(MOTION)) {
            caps.add(Capability.MOTION);
        }
        if (raw.hasCapability(CONTACT)) {
            caps.add(Capability.CONTACT);
        }
        if (raw.hasCapability(BATTERY)) {
            caps.add(Capability.BATTERY);
        }
        if (raw.hasCapability(POWER_METER)) {
            caps.add(Capability.POWER_METER);
        }
        if (raw.hasCapability(VIDEO_CAMERA) || raw.hasCapability(VIDEO_STREAM)) {
            caps.add(Capability.VIDEO);
        }
        return List.copyOf(caps);
    }

    // --------------------------------------------------------------- state

    private DeviceState resolveState(StDevice raw, DeviceType type, StDeviceStatus s) {
        DeviceState.Builder b = DeviceState.builder();

        string(s, SWITCH, "switch").ifPresent(v -> b.power("on".equalsIgnoreCase(v) ? PowerState.ON : PowerState.OFF));

        if (type == DeviceType.SHUTTER) {
            Optional<Integer> level = integer(s, WINDOW_SHADE_LEVEL, "shadeLevel")
                    .or(() -> integer(s, SWITCH_LEVEL, "level"));
            level.ifPresent(b::position);
            string(s, WINDOW_SHADE, "windowShade").map(DeviceNormalizer::openState)
                    .or(() -> level.map(DeviceNormalizer::openStateFromPosition))
                    .ifPresent(b::openState);
        } else {
            integer(s, SWITCH_LEVEL, "level").ifPresent(b::brightness);
        }

        integer(s, COLOR_TEMPERATURE, "colorTemperature").ifPresent(b::colorTemperature);
        Optional<Double> hue = number(s, COLOR_CONTROL, "hue");
        Optional<Double> sat = number(s, COLOR_CONTROL, "saturation");
        if (hue.isPresent() && sat.isPresent()) {
            // SmartThings usa una scala 0..100 per la tonalità.
            b.color(new ColorValue((int) Math.round(hue.get() * 3.6), (int) Math.round(sat.get())));
        }

        s.attribute(TEMPERATURE, "temperature").ifPresent(a -> {
            toDouble(a.value()).ifPresent(b::temperature);
            b.temperatureUnit(a.unit() == null ? "C" : a.unit());
        });
        number(s, HUMIDITY, "humidity").ifPresent(b::humidity);

        if (raw.hasCapability(THERMOSTAT_MODE) || raw.hasCapability(THERMOSTAT_HEATING_SETPOINT)
                || raw.hasCapability(THERMOSTAT_COOLING_SETPOINT)) {
            Optional<String> mode = string(s, THERMOSTAT_MODE, "thermostatMode");
            mode.ifPresent(b::thermostatMode);
            s.attribute(THERMOSTAT_MODE, "supportedThermostatModes")
                    .map(a -> a.value() instanceof Collection<?> c ? c.stream().map(Object::toString).toList() : null)
                    .ifPresent(b::supportedThermostatModes);
            boolean cooling = mode.map(m -> m.toLowerCase(Locale.ROOT).contains("cool")).orElse(false);
            Optional<Double> heat = number(s, THERMOSTAT_HEATING_SETPOINT, "heatingSetpoint");
            Optional<Double> cool = number(s, THERMOSTAT_COOLING_SETPOINT, "coolingSetpoint");
            (cooling ? cool.or(() -> heat) : heat.or(() -> cool)).ifPresent(b::targetTemperature);
            string(s, THERMOSTAT_OPERATING_STATE, "thermostatOperatingState")
                    .map(DeviceNormalizer::operatingState).ifPresent(b::operatingState);
        }

        string(s, MOTION, "motion").ifPresent(v -> b.motion("active".equalsIgnoreCase(v)));
        string(s, CONTACT, "contact").ifPresent(v -> b.contact("open".equalsIgnoreCase(v) ? ContactState.OPEN : ContactState.CLOSED));
        integer(s, BATTERY, "battery").ifPresent(b::battery);
        number(s, POWER_METER, "power").ifPresent(b::powerUsage);
        return b.build();
    }

    static OpenState openState(String value) {
        return switch (value.toLowerCase(Locale.ROOT).replace('_', ' ')) {
            case "open" -> OpenState.OPEN;
            case "closed" -> OpenState.CLOSED;
            case "opening" -> OpenState.OPENING;
            case "closing" -> OpenState.CLOSING;
            case "partially open" -> OpenState.PARTIALLY_OPEN;
            default -> OpenState.UNKNOWN;
        };
    }

    static OpenState openStateFromPosition(int position) {
        if (position <= 0) {
            return OpenState.CLOSED;
        }
        return position >= 100 ? OpenState.OPEN : OpenState.PARTIALLY_OPEN;
    }

    static OperatingState operatingState(String value) {
        String v = value.toLowerCase(Locale.ROOT);
        if (v.contains("heat")) {
            return OperatingState.HEATING;
        }
        if (v.contains("cool")) {
            return OperatingState.COOLING;
        }
        if (v.contains("fan")) {
            return OperatingState.FAN;
        }
        if (v.contains("idle") || v.contains("off")) {
            return OperatingState.IDLE;
        }
        return OperatingState.UNKNOWN;
    }

    // ------------------------------------------------------------- helpers

    private static Optional<String> string(StDeviceStatus s, String capability, String attribute) {
        return s.attribute(capability, attribute).map(a -> a.value().toString());
    }

    private static Optional<Double> number(StDeviceStatus s, String capability, String attribute) {
        return s.attribute(capability, attribute).flatMap(a -> toDouble(a.value()));
    }

    private static Optional<Integer> integer(StDeviceStatus s, String capability, String attribute) {
        return number(s, capability, attribute).map(d -> (int) Math.round(d));
    }

    private static Optional<Double> toDouble(Object value) {
        if (value instanceof Number n) {
            return Optional.of(n.doubleValue());
        }
        if (value instanceof String str) {
            try {
                return Optional.of(Double.parseDouble(str));
            } catch (NumberFormatException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }
}
