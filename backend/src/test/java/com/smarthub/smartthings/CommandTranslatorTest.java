package com.smarthub.smartthings;

import com.smarthub.model.CommandType;
import com.smarthub.model.DeviceCommand;
import com.smarthub.model.DeviceState;
import com.smarthub.model.DeviceType;
import com.smarthub.model.OpenState;
import com.smarthub.model.PowerState;
import com.smarthub.smartthings.api.StCommand;
import com.smarthub.smartthings.api.StDevice;
import com.smarthub.smartthings.mapping.CommandTranslator;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandTranslatorTest {

    private final CommandTranslator translator = new CommandTranslator();

    @Test
    void lightCommands() {
        StDevice light = DeviceNormalizerTest.device("l", "Light", "switch", "switchLevel", "colorControl");

        assertEquals(List.of(StCommand.of("switch", "on")),
                translator.translate(light, DeviceType.LIGHT, new DeviceCommand(CommandType.TURN_ON, null)));
        assertEquals(List.of(StCommand.of("switchLevel", "setLevel", 100)),
                translator.translate(light, DeviceType.LIGHT, new DeviceCommand(CommandType.SET_BRIGHTNESS, 140)));
        assertEquals(List.of(StCommand.of("colorControl", "setColor", Map.of("hue", 50, "saturation", 70))),
                translator.translate(light, DeviceType.LIGHT,
                        new DeviceCommand(CommandType.SET_COLOR, Map.of("hue", 180, "saturation", 70))));
    }

    @Test
    void unsupportedCommandIsRejected() {
        StDevice plug = DeviceNormalizerTest.device("p", "SmartPlug", "switch");
        assertThrows(CommandTranslator.UnsupportedCommandException.class,
                () -> translator.translate(plug, DeviceType.SWITCH, new DeviceCommand(CommandType.SET_BRIGHTNESS, 50)));
    }

    @Test
    void shutterCommandsPreferWindowShade() {
        StDevice shutter = DeviceNormalizerTest.device("s", "Blind", "windowShade", "windowShadeLevel");
        assertEquals(List.of(StCommand.of("windowShade", "open")),
                translator.translate(shutter, DeviceType.BLIND, new DeviceCommand(CommandType.OPEN, null)));
        assertEquals(List.of(StCommand.of("windowShade", "pause")),
                translator.translate(shutter, DeviceType.BLIND, new DeviceCommand(CommandType.STOP, null)));
        assertEquals(List.of(StCommand.of("windowShadeLevel", "setShadeLevel", 40)),
                translator.translate(shutter, DeviceType.BLIND, new DeviceCommand(CommandType.SET_POSITION, "40")));
    }

    @Test
    void thermostatSetpointRoundsToHalfDegree() {
        StDevice thermostat = DeviceNormalizerTest.device("t", "Thermostat", "thermostatHeatingSetpoint", "thermostatMode");
        assertEquals(List.of(StCommand.of("thermostatHeatingSetpoint", "setHeatingSetpoint", 21.5)),
                translator.translate(thermostat, DeviceType.THERMOSTAT,
                        new DeviceCommand(CommandType.SET_TARGET_TEMPERATURE, 21.4)));
    }

    @Test
    void predictionReflectsCommand() {
        DeviceState off = DeviceState.builder().power(PowerState.OFF).brightness(0).build();
        DeviceState predicted = translator.predict(off, new DeviceCommand(CommandType.SET_BRIGHTNESS, 60));
        assertEquals(PowerState.ON, predicted.power());
        assertEquals(60, predicted.brightness());

        DeviceState shutter = DeviceState.builder().position(20).openState(OpenState.PARTIALLY_OPEN).build();
        DeviceState moving = translator.predict(shutter, new DeviceCommand(CommandType.SET_POSITION, 80));
        assertEquals(80, moving.position());
        assertEquals(OpenState.OPENING, moving.openState());
    }
}
