package com.smarthub.smartthings.error;

/** Tipo di operazione verso SmartThings: cambia il significato di alcuni errori (es. offline solo per device). */
public enum SmartThingsOperation {
    READ,
    COMMAND,
    SCENE,
    AUTH
}
