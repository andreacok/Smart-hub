package com.smarthub.exception;

import org.springframework.http.HttpStatus;

/** Errore durante la gestione della fotografia di una stanza, con stato HTTP e codice espliciti. */
public class RoomImageException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public RoomImageException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}
