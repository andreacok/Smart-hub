package com.smarthub.smartthings.api;

/**
 * Regola (automazione con trigger) da GET /rules?locationId=. Solo i campi usati da Smart Hub:
 * gli altri vengono ignorati in deserializzazione.
 */
public record StRule(String id, String name) {
}
