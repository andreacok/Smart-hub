package com.smarthub.error;

/**
 * Codici applicativi stabili, letti dal frontend al posto dello status HTTP.
 * Ogni codice porta lo status HTTP con cui viene esposto, la sorgente, se è ritentabile e il messaggio per l'utente.
 * Aggiungere un codice significa aggiungere una riga qui e, se serve, un caso in {@code SmartThingsErrorMapper}.
 */
public enum ApiErrorCode {

    SMARTTHINGS_UNAUTHORIZED(502, ErrorSource.SMARTTHINGS, false,
            "La connessione a SmartThings non è autorizzata. Verifica le credenziali configurate."),
    SMARTTHINGS_FORBIDDEN(502, ErrorSource.SMARTTHINGS, false,
            "SmartThings non consente questa operazione con i permessi attuali."),
    SMARTTHINGS_DEVICE_NOT_FOUND(404, ErrorSource.SMARTTHINGS, false,
            "Il dispositivo non è più disponibile in SmartThings."),
    SMARTTHINGS_DEVICE_OFFLINE(424, ErrorSource.SMARTTHINGS, true,
            "Il dispositivo non è raggiungibile tramite SmartThings."),
    SMARTTHINGS_CAPABILITY_NOT_SUPPORTED(422, ErrorSource.SMARTTHINGS, false,
            "Questo dispositivo non supporta questa funzione."),
    SMARTTHINGS_COMMAND_NOT_SUPPORTED(422, ErrorSource.SMARTTHINGS, false,
            "Questo dispositivo non supporta questo comando."),
    SMARTTHINGS_COMMAND_FAILED(502, ErrorSource.SMARTTHINGS, false,
            "SmartThings non è riuscito a eseguire il comando sul dispositivo."),
    SMARTTHINGS_RATE_LIMITED(429, ErrorSource.SMARTTHINGS, true,
            "Troppe richieste a SmartThings. Riprova tra poco."),
    SMARTTHINGS_TIMEOUT(504, ErrorSource.NETWORK, true,
            "SmartThings non ha risposto in tempo."),
    SMARTTHINGS_UNAVAILABLE(503, ErrorSource.SMARTTHINGS, true,
            "SmartThings è temporaneamente non disponibile."),
    SMARTTHINGS_BAD_RESPONSE(502, ErrorSource.SMARTTHINGS, false,
            "SmartThings ha inviato una risposta non valida."),
    SMARTTHINGS_NETWORK_ERROR(502, ErrorSource.NETWORK, true,
            "Impossibile raggiungere SmartThings. Controlla la connessione."),
    SMARTTHINGS_ERROR(502, ErrorSource.SMARTTHINGS, false,
            "SmartThings ha restituito un errore."),

    SMART_HUB_VALIDATION_ERROR(400, ErrorSource.SMART_HUB, false,
            "La richiesta non è valida."),
    SMART_HUB_NOT_FOUND(404, ErrorSource.SMART_HUB, false,
            "Risorsa non trovata."),
    SMART_HUB_INTERNAL_ERROR(500, ErrorSource.SMART_HUB, false,
            "Si è verificato un errore inatteso.");

    private final int httpStatus;
    private final ErrorSource source;
    private final boolean retryable;
    private final String defaultMessage;

    ApiErrorCode(int httpStatus, ErrorSource source, boolean retryable, String defaultMessage) {
        this.httpStatus = httpStatus;
        this.source = source;
        this.retryable = retryable;
        this.defaultMessage = defaultMessage;
    }

    public int httpStatus() {
        return httpStatus;
    }

    public ErrorSource source() {
        return source;
    }

    public boolean retryable() {
        return retryable;
    }

    public String defaultMessage() {
        return defaultMessage;
    }
}
