package com.smarthub.smartthings.api;

import com.smarthub.smartthings.error.SmartThingsError;

/**
 * Errore nella comunicazione con SmartThings. Porta un {@link SmartThingsError} già classificato:
 * il {@code GlobalExceptionHandler} lo converte nella risposta REST senza doverlo interpretare di nuovo.
 */
public class SmartThingsException extends RuntimeException {

    private final transient SmartThingsError error;

    public SmartThingsException(SmartThingsError error) {
        this(error, null);
    }

    public SmartThingsException(SmartThingsError error, Throwable cause) {
        super(error.message(), cause);
        this.error = error;
    }

    public SmartThingsError error() {
        return error;
    }
}
