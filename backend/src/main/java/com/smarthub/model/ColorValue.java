package com.smarthub.model;

/**
 * Colore normalizzato.
 *
 * @param hue        tonalità 0..360
 * @param saturation saturazione 0..100
 */
public record ColorValue(int hue, int saturation) {
}
