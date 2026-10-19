package com.smarthub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.smarthub.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Persistenza delle fotografie personalizzate delle stanze, sotto {@code <SMARTHUB_DATA_DIR>/room-images/}.
 *
 * <ul>
 *   <li>Chiave: {@code roomId}, mai il nome della stanza (il nome può cambiare in SmartThings).</li>
 *   <li>Il nome del file sul disco è un UUID generato dal server: il nome originale non viene mai usato.</li>
 *   <li>L'indice {@code index.json} collega roomId → file. Ogni scrittura è atomica (file temporaneo + move).</li>
 * </ul>
 */
@Component
public class RoomImageStore {

    private static final Logger log = LoggerFactory.getLogger(RoomImageStore.class);
    private static final String INDEX_FILE = "index.json";
    private static final Pattern STORED_NAME = Pattern.compile("[0-9a-f-]{36}\\.jpg");

    /** Riferimento persistente a una fotografia: nome file sul disco e istante dell'ultimo upload. */
    public record Entry(String fileName, Instant updatedAt) {
    }

    private final Path directory;
    private final ObjectMapper mapper;
    private final Map<String, Entry> index = new HashMap<>();

    public RoomImageStore(AppProperties properties, ObjectMapper mapper) {
        this.mapper = mapper;
        this.directory = Path.of(properties.getDataDir()).toAbsolutePath().normalize().resolve("room-images");
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException("Impossibile creare la cartella delle foto stanze: " + directory, e);
        }
        loadIndex();
        log.info("Foto stanze personalizzate in {} ({} stanze)", directory, index.size());
    }

    public synchronized Optional<Entry> find(String roomId) {
        return Optional.ofNullable(index.get(roomId));
    }

    /** Legge il contenuto JPEG di una fotografia salvata. */
    public synchronized byte[] read(Entry entry) {
        Path file = fileOf(entry);
        try {
            return Files.readAllBytes(file);
        } catch (IOException e) {
            throw new IllegalStateException("Foto stanza non leggibile: " + file, e);
        }
    }

    /** Salva un nuovo JPEG per la stanza e rimuove quello precedente, dopo che l'indice è stato aggiornato. */
    public synchronized Entry put(String roomId, byte[] jpeg) {
        String fileName = UUID.randomUUID() + ".jpg";
        Path target = directory.resolve(fileName);
        writeAtomically(target, jpeg);

        Entry previous = index.get(roomId);
        index.put(roomId, new Entry(fileName, Instant.now()));
        persistIndex();
        if (previous != null) {
            deleteQuietly(fileOf(previous));
        }
        return index.get(roomId);
    }

    /** Rimuove la fotografia personalizzata della stanza. Non tocca mai gli asset built-in. */
    public synchronized void remove(String roomId) {
        Entry previous = index.remove(roomId);
        if (previous == null) {
            return;
        }
        persistIndex();
        deleteQuietly(fileOf(previous));
    }

    private Path fileOf(Entry entry) {
        if (!STORED_NAME.matcher(entry.fileName()).matches()) {
            throw new IllegalStateException("Nome file di foto stanza non valido nell'indice");
        }
        return directory.resolve(entry.fileName());
    }

    private void loadIndex() {
        Path file = directory.resolve(INDEX_FILE);
        if (!Files.exists(file)) {
            return;
        }
        try {
            Map<String, Entry> loaded = mapper.readValue(file.toFile(), new TypeReference<Map<String, Entry>>() { });
            index.putAll(loaded);
        } catch (IOException e) {
            // Meglio fermarsi che perdere in silenzio le associazioni stanza → foto.
            throw new IllegalStateException("Indice delle foto stanze non leggibile: " + file, e);
        }
    }

    private void persistIndex() {
        try {
            byte[] json = mapper.writerWithDefaultPrettyPrinter().writeValueAsBytes(index);
            writeAtomically(directory.resolve(INDEX_FILE), json);
        } catch (IOException e) {
            throw new IllegalStateException("Impossibile salvare l'indice delle foto stanze", e);
        }
    }

    private static void writeAtomically(Path target, byte[] content) {
        try {
            Path temp = Files.createTempFile(target.getParent(), "upload-", ".tmp");
            Files.write(temp, content);
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            throw new IllegalStateException("Impossibile scrivere " + target.getFileName(), e);
        }
    }

    private static void deleteQuietly(Path file) {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            log.warn("Impossibile eliminare la foto {}: {}", file.getFileName(), e.getMessage());
        }
    }
}
