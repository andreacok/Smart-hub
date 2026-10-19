package com.smarthub.exception;

import com.smarthub.dto.ErrorResponse;
import com.smarthub.smartthings.api.SmartThingsException;
import com.smarthub.smartthings.mapping.CommandTranslator;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;

/** Gestione centralizzata degli errori: ogni errore diventa un {@link ErrorResponse} JSON. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(NotFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", e.getMessage(), request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> noResource(NoResourceFoundException e, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "NOT_FOUND", "Risorsa non trovata", request);
    }

    @ExceptionHandler({BadRequestException.class, IllegalArgumentException.class,
            CommandTranslator.UnsupportedCommandException.class})
    public ResponseEntity<ErrorResponse> badRequest(RuntimeException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> invalid(MethodArgumentNotValidException e, HttpServletRequest request) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .findFirst().orElse("Richiesta non valida");
        return build(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "Body della richiesta non valido", request);
    }

    @ExceptionHandler(DeviceOfflineException.class)
    public ResponseEntity<ErrorResponse> offline(DeviceOfflineException e, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "DEVICE_OFFLINE", e.getMessage(), request);
    }

    @ExceptionHandler(SmartThingsException.class)
    public ResponseEntity<ErrorResponse> smartThings(SmartThingsException e, HttpServletRequest request) {
        HttpStatus status = switch (e.getStatus()) {
            case 404 -> HttpStatus.NOT_FOUND;
            case 409 -> HttpStatus.CONFLICT;
            case 422 -> HttpStatus.UNPROCESSABLE_ENTITY;
            case 429 -> HttpStatus.TOO_MANY_REQUESTS;
            case 504 -> HttpStatus.GATEWAY_TIMEOUT;
            default -> HttpStatus.BAD_GATEWAY;
        };
        log.warn("Errore SmartThings su {}: {}", request.getRequestURI(), e.getMessage());
        return build(status, "SMARTTHINGS_ERROR", e.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> unexpected(Exception e, HttpServletRequest request) {
        log.error("Errore inatteso su {}", request.getRequestURI(), e);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Errore interno del server", request);
    }

    private static ResponseEntity<ErrorResponse> build(HttpStatus status, String code, String message,
                                                       HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), code, message, request.getRequestURI(), Instant.now()));
    }
}
