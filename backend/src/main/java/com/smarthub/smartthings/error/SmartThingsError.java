package com.smarthub.smartthings.error;

import com.smarthub.error.ApiErrorCode;
import com.smarthub.error.ErrorSource;

/**
 * Errore applicativo prodotto dal {@link SmartThingsErrorMapper}: è il dato che attraversa il backend
 * e arriva al frontend. Non contiene mai token, header di autorizzazione o body grezzo di SmartThings.
 *
 * @param operation        operazione che è fallita
 * @param code             codice applicativo stabile
 * @param message          messaggio per l'utente, già pronto da mostrare
 * @param detail           dettaglio tecnico sanificato (può essere null)
 * @param deviceId         dispositivo coinvolto, quando noto
 * @param upstreamRequestId requestId restituito da SmartThings, per il supporto
 * @param upstreamCode     codice di errore originale di SmartThings (es. ConstraintViolationError)
 * @param upstreamStatus   status HTTP originale di SmartThings, null se non c'era una risposta HTTP
 * @param retryAfterSeconds valore di Retry-After, quando SmartThings lo fornisce
 */
public record SmartThingsError(
        SmartThingsOperation operation,
        ApiErrorCode code,
        String message,
        String detail,
        String deviceId,
        String upstreamRequestId,
        String upstreamCode,
        Integer upstreamStatus,
        Long retryAfterSeconds
) {

    public ErrorSource source() {
        return code.source();
    }

    public boolean retryable() {
        return code.retryable();
    }

    public int httpStatus() {
        return code.httpStatus();
    }
}
