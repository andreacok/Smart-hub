package com.smarthub.smartthings.error;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthub.error.ApiErrorCode;
import org.springframework.core.codec.DecodingException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeoutException;
import java.util.regex.Pattern;

/**
 * Unico punto che traduce un errore SmartThings in un {@link SmartThingsError} applicativo.
 * <p>
 * Regole:
 * <ul>
 *   <li>lo status HTTP è la base della classificazione;</li>
 *   <li>il body viene letto per il codice, il messaggio e il requestId di SmartThings, ma mai inoltrato grezzo;</li>
 *   <li>"offline" si riconosce solo quando il body o il contesto lo dicono, mai dal solo status HTTP;</li>
 *   <li>in caso di dubbio il risultato è {@code SMARTTHINGS_COMMAND_FAILED} o {@code SMARTTHINGS_ERROR},
 *       con il dettaglio tecnico sanificato.</li>
 * </ul>
 * Classe senza stato e senza dipendenze da Spring, quindi testabile in isolamento.
 */
public final class SmartThingsErrorMapper {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final int MAX_DETAIL_LENGTH = 300;

    private static final Pattern BEARER = Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._~+/=-]+");
    private static final Pattern SECRET_FIELD = Pattern.compile(
            "(?i)(access_token|refresh_token|client_secret|authorization|token|password|secret)([\"'\\s]*[=:][\"'\\s]*)[^\\s,\"'}&]+");
    private static final Pattern URL = Pattern.compile("https?://\\S+");

    private SmartThingsErrorMapper() {
    }

    /** Errore con risposta HTTP di SmartThings. */
    public static SmartThingsError fromHttp(SmartThingsOperation operation, String deviceId, int status,
                                            String body, Long retryAfterSeconds) {
        Upstream upstream = Upstream.parse(body);
        ApiErrorCode code = classify(operation, deviceId, status, upstream.searchText());
        String detail = upstream.message() != null
                ? sanitize(upstream.message())
                : "SmartThings ha risposto con HTTP " + status;
        return new SmartThingsError(operation, code, code.defaultMessage(), detail, deviceId,
                upstream.requestId(), upstream.code(), status, retryAfterSeconds);
    }

    /** Errore generico da un'eccezione che non ha una risposta HTTP: timeout, rete, decodifica, altro. */
    public static SmartThingsError fromException(SmartThingsOperation operation, String deviceId, Throwable e) {
        if (containsCause(e, TimeoutException.class)) {
            return timeout(operation, deviceId);
        }
        if (e instanceof WebClientResponseException r) {
            return fromHttp(operation, deviceId, r.getStatusCode().value(), r.getResponseBodyAsString(),
                    parseRetryAfter(r.getHeaders().getFirst("Retry-After")));
        }
        if (containsCause(e, DecodingException.class) || containsCause(e, JsonProcessingException.class)) {
            return badResponse(operation, deviceId, "Impossibile leggere la risposta di SmartThings");
        }
        return network(operation, deviceId, e);
    }

    public static SmartThingsError timeout(SmartThingsOperation operation, String deviceId) {
        return new SmartThingsError(operation, ApiErrorCode.SMARTTHINGS_TIMEOUT, ApiErrorCode.SMARTTHINGS_TIMEOUT.defaultMessage(),
                "Nessuna risposta entro il tempo massimo configurato", deviceId, null, null, null, null);
    }

    public static SmartThingsError network(SmartThingsOperation operation, String deviceId, Throwable e) {
        String cause = e.getClass().getSimpleName() + (e.getMessage() == null ? "" : ": " + e.getMessage());
        return new SmartThingsError(operation, ApiErrorCode.SMARTTHINGS_NETWORK_ERROR,
                ApiErrorCode.SMARTTHINGS_NETWORK_ERROR.defaultMessage(), sanitize(cause), deviceId, null, null, null, null);
    }

    public static SmartThingsError badResponse(SmartThingsOperation operation, String deviceId, String detail) {
        return new SmartThingsError(operation, ApiErrorCode.SMARTTHINGS_BAD_RESPONSE,
                ApiErrorCode.SMARTTHINGS_BAD_RESPONSE.defaultMessage(), sanitize(detail), deviceId, null, null, null, null);
    }

    /** Errore con codice scelto dal chiamante e messaggio/dettaglio espliciti (configurazione, casi locali). */
    public static SmartThingsError custom(SmartThingsOperation operation, ApiErrorCode code, String message, String detail) {
        return new SmartThingsError(operation, code, message == null ? code.defaultMessage() : message,
                sanitize(detail), null, null, null, null, null);
    }

    /**
     * Rimuove dal payload i segreti noti al client (PAT, token OAuth in memoria). Il pattern non basta:
     * un token può comparire nudo nel messaggio di SmartThings, quindi si cerca il valore esatto.
     */
    public static SmartThingsError redact(SmartThingsError error, List<String> secrets) {
        if (secrets == null || secrets.isEmpty()) {
            return error;
        }
        return new SmartThingsError(error.operation(), error.code(),
                scrub(error.message(), secrets), scrub(error.detail(), secrets), error.deviceId(),
                scrub(error.upstreamRequestId(), secrets), scrub(error.upstreamCode(), secrets),
                error.upstreamStatus(), error.retryAfterSeconds());
    }

    private static String scrub(String text, List<String> secrets) {
        if (text == null) {
            return null;
        }
        String clean = text;
        for (String secret : secrets) {
            if (secret != null && secret.length() >= 8) {
                clean = clean.replace(secret, "***");
            }
        }
        return clean;
    }

    /**
     * Corpo di risposta fittizio per le simulazioni (mock): passa dalla stessa classificazione del reale.
     */
    public static String simulatedBody(String message) {
        return "{\"error\":{\"message\":\"" + message.replace("\"", "'") + "\"}}";
    }

    // ------------------------------------------------------------ classification

    static ApiErrorCode classify(SmartThingsOperation operation, String deviceId, int status, String searchText) {
        if (operation == SmartThingsOperation.AUTH && (status == 400 || status == 401)) {
            return ApiErrorCode.SMARTTHINGS_UNAUTHORIZED;
        }
        switch (status) {
            case 401:
                return ApiErrorCode.SMARTTHINGS_UNAUTHORIZED;
            case 403:
                return ApiErrorCode.SMARTTHINGS_FORBIDDEN;
            case 429:
                return ApiErrorCode.SMARTTHINGS_RATE_LIMITED;
            default:
                break;
        }
        if (status >= 500) {
            return ApiErrorCode.SMARTTHINGS_UNAVAILABLE;
        }
        boolean onDevice = deviceId != null && (operation == SmartThingsOperation.READ
                || operation == SmartThingsOperation.COMMAND);
        if (status == 404) {
            return onDevice ? ApiErrorCode.SMARTTHINGS_DEVICE_NOT_FOUND : ApiErrorCode.SMARTTHINGS_ERROR;
        }
        if (onDevice && searchText.contains("offline")) {
            return ApiErrorCode.SMARTTHINGS_DEVICE_OFFLINE;
        }
        if (operation == SmartThingsOperation.COMMAND && (status == 400 || status == 409 || status == 422)) {
            if (mentionsUnsupported(searchText)) {
                boolean aboutCommand = searchText.contains("command") || searchText.contains("comando");
                return aboutCommand ? ApiErrorCode.SMARTTHINGS_COMMAND_NOT_SUPPORTED
                        : ApiErrorCode.SMARTTHINGS_CAPABILITY_NOT_SUPPORTED;
            }
            return ApiErrorCode.SMARTTHINGS_COMMAND_FAILED;
        }
        return ApiErrorCode.SMARTTHINGS_ERROR;
    }

    private static boolean mentionsUnsupported(String text) {
        return text.contains("not supported") || text.contains("unsupported")
                || text.contains("non supportat") || text.contains("not support");
    }

    // ------------------------------------------------------------ helpers

    static String sanitize(String text) {
        if (text == null) {
            return null;
        }
        String clean = BEARER.matcher(text).replaceAll("Bearer ***");
        clean = SECRET_FIELD.matcher(clean).replaceAll("$1$2***");
        clean = URL.matcher(clean).replaceAll("<url>");
        clean = clean.replaceAll("\\s+", " ").trim();
        if (clean.length() > MAX_DETAIL_LENGTH) {
            clean = clean.substring(0, MAX_DETAIL_LENGTH) + "…";
        }
        return clean;
    }

    static Long parseRetryAfter(String value) {
        if (value == null) {
            return null;
        }
        try {
            long seconds = Long.parseLong(value.trim());
            return seconds >= 0 ? seconds : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static boolean containsCause(Throwable e, Class<? extends Throwable> type) {
        for (Throwable current = e; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
            if (current.getCause() == current) {
                break;
            }
        }
        return false;
    }

    /** Campi utili del body di errore SmartThings: {@code {"requestId": "...", "error": {"code", "message", "details"}}}. */
    private record Upstream(String requestId, String code, String message, String details) {

        static Upstream parse(String body) {
            if (body == null || body.isBlank()) {
                return new Upstream(null, null, null, null);
            }
            try {
                JsonNode root = JSON.readTree(body);
                JsonNode error = root.path("error");
                String message = text(error.path("message"));
                if (message == null) {
                    message = text(root.path("message"));
                }
                StringBuilder details = new StringBuilder();
                for (JsonNode d : error.path("details")) {
                    String m = text(d.path("message"));
                    if (m != null) {
                        details.append(' ').append(m);
                    }
                }
                return new Upstream(text(root.path("requestId")), text(error.path("code")), message,
                        details.toString().trim());
            } catch (JsonProcessingException e) {
                // Body non JSON (es. HTML di un proxy): nessun dettaglio utile, si usa solo lo status.
                return new Upstream(null, null, null, null);
            }
        }

        /** Testo in minuscolo su cui cercare parole chiave (offline, command, not supported...). */
        String searchText() {
            return ((code == null ? "" : code) + " " + (message == null ? "" : message) + " "
                    + (details == null ? "" : details)).toLowerCase(Locale.ROOT);
        }

        private static String text(JsonNode node) {
            return node == null || node.isMissingNode() || node.isNull() || node.asText().isBlank()
                    ? null : node.asText();
        }
    }
}
