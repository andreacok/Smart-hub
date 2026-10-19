package com.smarthub.exception;

import com.smarthub.config.CorrelationIdFilter;
import com.smarthub.dto.ErrorResponse;
import com.smarthub.error.ApiErrorCode;
import com.smarthub.error.ErrorSource;
import com.smarthub.smartthings.api.SmartThingsException;
import com.smarthub.smartthings.error.SmartThingsError;
import com.smarthub.smartthings.mapping.CommandTranslator;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;

/**
 * Gestione centralizzata degli errori: ogni errore diventa un {@link ErrorResponse} JSON.
 * Gli errori SmartThings arrivano già classificati dal {@code SmartThingsErrorMapper}: qui si serializzano soltanto.
 * Nessun stack trace o HTML di errore raggiunge il frontend.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(SmartThingsException.class)
    public ResponseEntity<ErrorResponse> smartThings(SmartThingsException e, HttpServletRequest request) {
        SmartThingsError error = e.error();
        // Errore atteso e già classificato: una riga di log, senza stack trace.
        log.warn("SmartThings request failed: operation={} deviceId={} upstreamStatus={} upstreamCode={} "
                        + "mappedCode={} upstreamRequestId={} correlationId={} path={}",
                error.operation(), error.deviceId(), error.upstreamStatus(), error.upstreamCode(), error.code(),
                error.upstreamRequestId(), correlationId(request), request.getRequestURI());
        if (error.detail() != null) {
            log.debug("Dettaglio SmartThings ({}): {}", error.code(), error.detail());
        }
        return ResponseEntity.status(error.httpStatus()).body(new ErrorResponse(error.httpStatus(),
                error.code().name(), error.source().name(), error.message(), error.detail(), error.retryable(),
                error.deviceId(), error.retryAfterSeconds(), error.upstreamRequestId(), correlationId(request),
                request.getRequestURI(), Instant.now()));
    }

    @ExceptionHandler(DeviceOfflineException.class)
    public ResponseEntity<ErrorResponse> offline(DeviceOfflineException e, HttpServletRequest request) {
        log.warn("Comando non inviato: dispositivo offline deviceId={} correlationId={}", e.deviceId(),
                correlationId(request));
        return code(ApiErrorCode.SMARTTHINGS_DEVICE_OFFLINE, null,
                "Il dispositivo risulta offline nell'ultimo stato letto da SmartThings.", e.deviceId(), request);
    }

    @ExceptionHandler(CommandTranslator.UnsupportedCommandException.class)
    public ResponseEntity<ErrorResponse> unsupportedCommand(CommandTranslator.UnsupportedCommandException e,
                                                            HttpServletRequest request) {
        return code(ApiErrorCode.SMARTTHINGS_COMMAND_NOT_SUPPORTED, e.getMessage(), null, null, request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(NotFoundException e, HttpServletRequest request) {
        return code(ApiErrorCode.SMART_HUB_NOT_FOUND, e.getMessage(), null, null, request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noResource(NoResourceFoundException e, HttpServletRequest request) {
        return code(ApiErrorCode.SMART_HUB_NOT_FOUND, "Risorsa non trovata", null, null, request);
    }

    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> badRequest(RuntimeException e, HttpServletRequest request) {
        return code(ApiErrorCode.SMART_HUB_VALIDATION_ERROR, e.getMessage(), null, null, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> invalid(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst().orElse(null);
        return code(ApiErrorCode.SMART_HUB_VALIDATION_ERROR, message, null, null, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        return code(ApiErrorCode.SMART_HUB_VALIDATION_ERROR, "Il corpo della richiesta non è valido.", null, null,
                request);
    }

    @ExceptionHandler(RoomImageException.class)
    public ResponseEntity<ErrorResponse> roomImage(RoomImageException e, HttpServletRequest request) {
        return raw(e.getStatus().value(), e.getCode(), e.getMessage(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> tooLarge(MaxUploadSizeExceededException e, HttpServletRequest request) {
        return raw(HttpStatus.PAYLOAD_TOO_LARGE.value(), "FILE_TOO_LARGE", "La foto supera il limite di 10 MB.", request);
    }

    @ExceptionHandler({MissingServletRequestPartException.class, MissingServletRequestParameterException.class})
    public ResponseEntity<ErrorResponse> missingFile(Exception e, HttpServletRequest request) {
        return code(ApiErrorCode.SMART_HUB_VALIDATION_ERROR, "Campo 'file' mancante nella richiesta.", null, null,
                request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> methodNotAllowed(HttpRequestMethodNotSupportedException e,
                                                          HttpServletRequest request) {
        return raw(HttpStatus.METHOD_NOT_ALLOWED.value(), "METHOD_NOT_ALLOWED", "Metodo HTTP non consentito.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> unsupportedMedia(HttpMediaTypeNotSupportedException e,
                                                          HttpServletRequest request) {
        return raw(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(), "UNSUPPORTED_MEDIA_TYPE",
                "Tipo di contenuto non supportato.", request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest request) {
        // Errore inatteso del nostro backend: qui lo stack trace serve, ma non va mai al frontend.
        log.error("Errore inatteso su {} correlationId={}", request.getRequestURI(), correlationId(request), e);
        return code(ApiErrorCode.SMART_HUB_INTERNAL_ERROR, null, null, null, request);
    }

    // ------------------------------------------------------------ helpers

    private static ResponseEntity<ErrorResponse> code(ApiErrorCode code, String message, String detail,
                                                      String deviceId, HttpServletRequest request) {
        return ResponseEntity.status(code.httpStatus()).body(body(code.httpStatus(), code.name(), code.source(),
                message == null ? code.defaultMessage() : message, detail, code.retryable(), deviceId, null, null,
                request));
    }

    private static ResponseEntity<ErrorResponse> raw(int status, String code, String message, HttpServletRequest request) {
        return ResponseEntity.status(status).body(body(status, code, ErrorSource.SMART_HUB, message, null, false,
                null, null, null, request));
    }

    private static ErrorResponse body(int status, String code, ErrorSource source, String message, String detail,
                                      boolean retryable, String deviceId, Long retryAfter, String upstreamRequestId,
                                      HttpServletRequest request) {
        return new ErrorResponse(status, code, source.name(), message, detail, retryable, deviceId, retryAfter,
                upstreamRequestId, correlationId(request), request.getRequestURI(), Instant.now());
    }

    private static String correlationId(HttpServletRequest request) {
        Object fromRequest = request.getAttribute(CorrelationIdFilter.ATTRIBUTE);
        if (fromRequest != null) {
            return fromRequest.toString();
        }
        return MDC.get(CorrelationIdFilter.ATTRIBUTE);
    }
}
