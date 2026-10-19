package com.smarthub.dto;

import java.time.Instant;

/** Formato uniforme degli errori restituiti dalle API. */
public record ErrorResponse(int status, String code, String message, String path, Instant timestamp) {
}
