package com.smarthub.model;

/** Comandi normalizzati che il frontend può inviare a un dispositivo. */
public enum CommandType {
    TURN_ON,
    TURN_OFF,
    /** value: 0..100 */
    SET_BRIGHTNESS,
    /** value: kelvin, es. 2700 */
    SET_COLOR_TEMPERATURE,
    /** value: { "hue": 0..360, "saturation": 0..100 } */
    SET_COLOR,
    OPEN,
    CLOSE,
    STOP,
    /** value: 0 (chiusa) .. 100 (aperta) */
    SET_POSITION,
    /** value: gradi, es. 21.5 */
    SET_TARGET_TEMPERATURE,
    /** value: es. "heat", "cool", "auto", "off" */
    SET_THERMOSTAT_MODE
}
