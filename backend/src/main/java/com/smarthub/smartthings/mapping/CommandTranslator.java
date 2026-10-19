package com.smarthub.smartthings.mapping;

import com.smarthub.model.ColorValue;
import com.smarthub.model.DeviceCommand;
import com.smarthub.model.DeviceState;
import com.smarthub.model.DeviceType;
import com.smarthub.model.OpenState;
import com.smarthub.model.PowerState;
import com.smarthub.smartthings.api.StCommand;
import com.smarthub.smartthings.api.StDevice;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.smarthub.smartthings.api.StCapabilities.*;

/**
 * Traduce un comando normalizzato nei comandi SmartThings e calcola lo stato atteso
 * (usato per aggiornare subito la cache senza attendere il refresh).
 */
public class CommandTranslator {

    public List<StCommand> translate(StDevice device, DeviceType type, DeviceCommand command) {
        return switch (command.type()) {
            case TURN_ON -> List.of(StCommand.of(require(device, SWITCH, command), "on"));
            case TURN_OFF -> List.of(StCommand.of(require(device, SWITCH, command), "off"));
            case SET_BRIGHTNESS -> {
                int level = DeviceCommand.clamp(command.intValue(), 0, 100);
                yield List.of(StCommand.of(require(device, SWITCH_LEVEL, command), "setLevel", level));
            }
            case SET_COLOR_TEMPERATURE -> {
                int kelvin = DeviceCommand.clamp(command.intValue(), 1000, 30000);
                yield List.of(StCommand.of(require(device, COLOR_TEMPERATURE, command), "setColorTemperature", kelvin));
            }
            case SET_COLOR -> {
                ColorValue c = command.colorValue();
                int hue = (int) Math.round(c.hue() / 3.6);
                yield List.of(StCommand.of(require(device, COLOR_CONTROL, command), "setColor",
                        Map.of("hue", hue, "saturation", c.saturation())));
            }
            case OPEN -> List.of(shadeCommand(device, command, "open", 100));
            case CLOSE -> List.of(shadeCommand(device, command, "close", 0));
            case STOP -> List.of(StCommand.of(require(device, WINDOW_SHADE, command), "pause"));
            case SET_POSITION -> {
                int position = DeviceCommand.clamp(command.intValue(), 0, 100);
                if (device.hasCapability(WINDOW_SHADE_LEVEL)) {
                    yield List.of(StCommand.of(WINDOW_SHADE_LEVEL, "setShadeLevel", position));
                }
                if (type == DeviceType.SHUTTER && device.hasCapability(SWITCH_LEVEL)) {
                    yield List.of(StCommand.of(SWITCH_LEVEL, "setLevel", position));
                }
                throw unsupported(command);
            }
            case SET_TARGET_TEMPERATURE -> {
                double target = Math.round(command.doubleValue() * 2) / 2.0; // passi da 0.5°
                if (device.hasCapability(THERMOSTAT_HEATING_SETPOINT)) {
                    yield List.of(StCommand.of(THERMOSTAT_HEATING_SETPOINT, "setHeatingSetpoint", target));
                }
                if (device.hasCapability(THERMOSTAT_COOLING_SETPOINT)) {
                    yield List.of(StCommand.of(THERMOSTAT_COOLING_SETPOINT, "setCoolingSetpoint", target));
                }
                throw unsupported(command);
            }
            case SET_THERMOSTAT_MODE -> List.of(StCommand.of(require(device, THERMOSTAT_MODE, command),
                    "setThermostatMode", command.stringValue().toLowerCase(Locale.ROOT)));
        };
    }

    /**
     * Variante del setpoint che tiene conto della modalità corrente (raffrescamento → cooling setpoint).
     */
    public List<StCommand> translate(StDevice device, DeviceType type, DeviceCommand command, DeviceState current) {
        if (command.type() == com.smarthub.model.CommandType.SET_TARGET_TEMPERATURE
                && current != null && current.thermostatMode() != null
                && current.thermostatMode().toLowerCase(Locale.ROOT).contains("cool")
                && device.hasCapability(THERMOSTAT_COOLING_SETPOINT)) {
            double target = Math.round(command.doubleValue() * 2) / 2.0;
            return List.of(StCommand.of(THERMOSTAT_COOLING_SETPOINT, "setCoolingSetpoint", target));
        }
        return translate(device, type, command);
    }

    /** Stato atteso dopo il comando: permette alla UI di vedere subito il risultato. */
    public DeviceState predict(DeviceState current, DeviceCommand command) {
        DeviceState.Builder b = current.toBuilder();
        switch (command.type()) {
            case TURN_ON -> b.power(PowerState.ON);
            case TURN_OFF -> b.power(PowerState.OFF);
            case SET_BRIGHTNESS -> {
                int level = DeviceCommand.clamp(command.intValue(), 0, 100);
                b.brightness(level);
                if (current.power() != null) {
                    b.power(level > 0 ? PowerState.ON : PowerState.OFF);
                }
            }
            case SET_COLOR_TEMPERATURE -> b.colorTemperature(command.intValue());
            case SET_COLOR -> b.color(command.colorValue());
            case OPEN -> b.openState(OpenState.OPENING);
            case CLOSE -> b.openState(OpenState.CLOSING);
            case STOP -> b.openState(current.position() == null ? OpenState.UNKNOWN
                    : DeviceNormalizer.openStateFromPosition(current.position()));
            case SET_POSITION -> {
                int target = DeviceCommand.clamp(command.intValue(), 0, 100);
                Integer from = current.position();
                b.position(target);
                b.openState(from == null || from.equals(target) ? DeviceNormalizer.openStateFromPosition(target)
                        : (target > from ? OpenState.OPENING : OpenState.CLOSING));
            }
            case SET_TARGET_TEMPERATURE -> b.targetTemperature(Math.round(command.doubleValue() * 2) / 2.0);
            case SET_THERMOSTAT_MODE -> b.thermostatMode(command.stringValue().toLowerCase(Locale.ROOT));
        }
        return b.build();
    }

    private static StCommand shadeCommand(StDevice device, DeviceCommand command, String stCommand, int level) {
        if (device.hasCapability(WINDOW_SHADE)) {
            return StCommand.of(WINDOW_SHADE, stCommand);
        }
        if (device.hasCapability(WINDOW_SHADE_LEVEL)) {
            return StCommand.of(WINDOW_SHADE_LEVEL, "setShadeLevel", level);
        }
        if (device.hasCapability(SWITCH_LEVEL)) {
            return StCommand.of(SWITCH_LEVEL, "setLevel", level);
        }
        throw unsupported(command);
    }

    private static String require(StDevice device, String capability, DeviceCommand command) {
        if (!device.hasCapability(capability)) {
            throw unsupported(command);
        }
        return capability;
    }

    private static UnsupportedCommandException unsupported(DeviceCommand command) {
        return new UnsupportedCommandException("Il dispositivo non supporta il comando " + command.type());
    }

    /** Comando non supportato dal dispositivo. */
    public static class UnsupportedCommandException extends RuntimeException {
        public UnsupportedCommandException(String message) {
            super(message);
        }
    }
}
