package com.smarthub.smartthings.api;

/** Valore di un attributo nello status: { "value": ..., "unit": "..." }. */
public record StAttribute(Object value, String unit) {
}
