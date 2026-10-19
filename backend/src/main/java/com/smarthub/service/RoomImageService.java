package com.smarthub.service;

import com.smarthub.dto.RoomImageDto;
import com.smarthub.dto.RoomSummaryDto;
import com.smarthub.exception.BadRequestException;
import com.smarthub.exception.NotFoundException;
import com.smarthub.model.Room;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Fotografie personalizzate delle stanze: upload, ripristino del default e lettura.
 *
 * La stanza è identificata dal {@code roomId} (stabile, anche se il nome cambia). Se la stanza non esiste
 * nella casa caricata, l'operazione viene rifiutata: niente foto orfane.
 */
@Service
public class RoomImageService {

    /** ID stanza ammessi: lettere, cifre e trattini. Blocca path traversal e caratteri strani. */
    private static final Pattern ROOM_ID = Pattern.compile("[A-Za-z0-9][A-Za-z0-9-]{0,63}");

    private final HomeStateCache cache;
    private final RoomImageStore store;
    private final RoomImageProcessor processor;

    public RoomImageService(HomeStateCache cache, RoomImageStore store, RoomImageProcessor processor) {
        this.cache = cache;
        this.store = store;
        this.processor = processor;
    }

    /** Valida, normalizza e salva la fotografia caricata, sostituendo quella precedente. */
    public RoomImageDto upload(String roomId, byte[] content) {
        requireRoom(roomId);
        byte[] jpeg = processor.process(content);
        RoomImageStore.Entry entry = store.put(roomId, jpeg);
        return new RoomImageDto(roomId, true, urlFor(roomId, entry.updatedAt()), entry.updatedAt());
    }

    /** Ripristina la fotografia predefinita: elimina solo la personalizzata, mai gli asset built-in. */
    public RoomImageDto reset(String roomId) {
        requireRoom(roomId);
        store.remove(roomId);
        return new RoomImageDto(roomId, false, null, null);
    }

    /** Contenuto JPEG e istante di aggiornamento, se la stanza ha una foto personalizzata. */
    public Optional<StoredImage> content(String roomId) {
        requireValidId(roomId);
        return store.find(roomId).map(e -> new StoredImage(store.read(e), e.updatedAt()));
    }

    /** Aggiunge alla summary l'URL della foto personalizzata, se presente. */
    public RoomSummaryDto decorate(RoomSummaryDto summary) {
        return store.find(summary.id())
                .map(e -> summary.withCustomImage(urlFor(summary.id(), e.updatedAt())))
                .orElse(summary);
    }

    /** URL versionato: cambia a ogni upload, quindi il browser non riusa la foto precedente. */
    static String urlFor(String roomId, Instant updatedAt) {
        return "/api/rooms/" + URLEncoder.encode(roomId, StandardCharsets.UTF_8)
                + "/image/content?v=" + updatedAt.toEpochMilli();
    }

    private void requireRoom(String roomId) {
        requireValidId(roomId);
        boolean exists = cache.rooms().stream().map(Room::id).anyMatch(roomId::equals);
        if (!exists) {
            throw new NotFoundException("Stanza non trovata: " + roomId);
        }
    }

    private static void requireValidId(String roomId) {
        if (roomId == null || !ROOM_ID.matcher(roomId).matches()) {
            throw new BadRequestException("roomId non valido");
        }
    }

    /** Contenuto della fotografia con l'istante di versione. */
    public record StoredImage(byte[] bytes, Instant updatedAt) {
    }
}
