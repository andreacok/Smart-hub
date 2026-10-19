package com.smarthub.smartthings.api;

/** Errore nella comunicazione con SmartThings (rete, timeout, autenticazione, risposta non valida). */
public class SmartThingsException extends RuntimeException {

    private final int status;

    public SmartThingsException(String message) {
        this(message, 0, null);
    }

    public SmartThingsException(String message, int status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    /** Status HTTP restituito da SmartThings, 0 se non disponibile. */
    public int getStatus() {
        return status;
    }
}
