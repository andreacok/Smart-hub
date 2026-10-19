package com.smarthub.dto;

import java.time.Instant;

/**
 * Stato della fotografia personalizzata di una stanza.
 * {@code imageUrl} è null quando la stanza usa la fotografia predefinita del frontend.
 */
public record RoomImageDto(String roomId, boolean customImage, String imageUrl, Instant updatedAt) {
}
