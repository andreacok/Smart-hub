package com.smarthub.smartthings.api;

/** Identificativi delle capability SmartThings usate dal backend. */
public final class StCapabilities {

    public static final String SWITCH = "switch";
    public static final String SWITCH_LEVEL = "switchLevel";
    public static final String COLOR_TEMPERATURE = "colorTemperature";
    public static final String COLOR_CONTROL = "colorControl";
    public static final String WINDOW_SHADE = "windowShade";
    public static final String WINDOW_SHADE_LEVEL = "windowShadeLevel";
    public static final String TEMPERATURE = "temperatureMeasurement";
    public static final String HUMIDITY = "relativeHumidityMeasurement";
    public static final String THERMOSTAT_MODE = "thermostatMode";
    public static final String THERMOSTAT_HEATING_SETPOINT = "thermostatHeatingSetpoint";
    public static final String THERMOSTAT_COOLING_SETPOINT = "thermostatCoolingSetpoint";
    public static final String THERMOSTAT_OPERATING_STATE = "thermostatOperatingState";
    public static final String MOTION = "motionSensor";
    public static final String CONTACT = "contactSensor";
    public static final String BATTERY = "battery";
    public static final String POWER_METER = "powerMeter";
    public static final String VIDEO_CAMERA = "videoCamera";
    public static final String VIDEO_STREAM = "videoStream";

    private StCapabilities() {
    }
}
