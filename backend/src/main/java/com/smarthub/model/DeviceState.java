package com.smarthub.model;

import java.util.List;

/**
 * Stato normalizzato di un dispositivo. Tutti i campi sono opzionali:
 * vengono valorizzati solo quelli supportati dal dispositivo (i null non vengono serializzati).
 */
public record DeviceState(
        PowerState power,
        Integer brightness,
        Integer colorTemperature,
        ColorValue color,
        OpenState openState,
        Integer position,
        Double temperature,
        String temperatureUnit,
        Double humidity,
        Double targetTemperature,
        String thermostatMode,
        List<String> supportedThermostatModes,
        OperatingState operatingState,
        Boolean motion,
        ContactState contact,
        Integer battery,
        Double powerUsage
) {

    public static DeviceState empty() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        Builder b = new Builder();
        b.power = power;
        b.brightness = brightness;
        b.colorTemperature = colorTemperature;
        b.color = color;
        b.openState = openState;
        b.position = position;
        b.temperature = temperature;
        b.temperatureUnit = temperatureUnit;
        b.humidity = humidity;
        b.targetTemperature = targetTemperature;
        b.thermostatMode = thermostatMode;
        b.supportedThermostatModes = supportedThermostatModes;
        b.operatingState = operatingState;
        b.motion = motion;
        b.contact = contact;
        b.battery = battery;
        b.powerUsage = powerUsage;
        return b;
    }

    public static final class Builder {
        private PowerState power;
        private Integer brightness;
        private Integer colorTemperature;
        private ColorValue color;
        private OpenState openState;
        private Integer position;
        private Double temperature;
        private String temperatureUnit;
        private Double humidity;
        private Double targetTemperature;
        private String thermostatMode;
        private List<String> supportedThermostatModes;
        private OperatingState operatingState;
        private Boolean motion;
        private ContactState contact;
        private Integer battery;
        private Double powerUsage;

        private Builder() {
        }

        public Builder power(PowerState v) { this.power = v; return this; }
        public Builder brightness(Integer v) { this.brightness = v; return this; }
        public Builder colorTemperature(Integer v) { this.colorTemperature = v; return this; }
        public Builder color(ColorValue v) { this.color = v; return this; }
        public Builder openState(OpenState v) { this.openState = v; return this; }
        public Builder position(Integer v) { this.position = v; return this; }
        public Builder temperature(Double v) { this.temperature = v; return this; }
        public Builder temperatureUnit(String v) { this.temperatureUnit = v; return this; }
        public Builder humidity(Double v) { this.humidity = v; return this; }
        public Builder targetTemperature(Double v) { this.targetTemperature = v; return this; }
        public Builder thermostatMode(String v) { this.thermostatMode = v; return this; }
        public Builder supportedThermostatModes(List<String> v) { this.supportedThermostatModes = v; return this; }
        public Builder operatingState(OperatingState v) { this.operatingState = v; return this; }
        public Builder motion(Boolean v) { this.motion = v; return this; }
        public Builder contact(ContactState v) { this.contact = v; return this; }
        public Builder battery(Integer v) { this.battery = v; return this; }
        public Builder powerUsage(Double v) { this.powerUsage = v; return this; }

        public DeviceState build() {
            return new DeviceState(power, brightness, colorTemperature, color, openState, position,
                    temperature, temperatureUnit, humidity, targetTemperature, thermostatMode,
                    supportedThermostatModes == null ? null : List.copyOf(supportedThermostatModes),
                    operatingState, motion, contact, battery, powerUsage);
        }
    }
}
