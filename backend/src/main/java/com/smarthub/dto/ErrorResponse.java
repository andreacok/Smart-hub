package com.smarthub.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Formato uniforme degli errori restituiti dalle API.
 * <ul>
 *   <li>{@code code}: codice applicativo stabile: il frontend decide il messaggio da questo, non dallo status HTTP;</li>
 *   <li>{@code source}: SMARTTHINGS, SMART_HUB, NETWORK o UNKNOWN;</li>
 *   <li>{@code message}: testo pronto per l'utente; {@code detail}: dettaglio tecnico sanificato, opzionale;</li>
 *   <li>{@code correlationId}: ID della richiesta Smart Hub; {@code upstreamRequestId}: requestId di SmartThings.</li>
 * </ul>
 * I campi opzionali vengono omessi quando non applicabili.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String code,
        String source,
        String message,
        String detail,
        boolean retryable,
        String deviceId,
        Long retryAfterSeconds,
        String upstreamRequestId,
        String correlationId,
        String path,
        Instant timestamp
) {
}
