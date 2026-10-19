package com.smarthub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthub.config.AppProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomImageStoreTest {

    @TempDir
    Path dataDir;

    private RoomImageStore newStore() {
        AppProperties props = new AppProperties();
        props.setDataDir(dataDir.toString());
        return new RoomImageStore(props, new ObjectMapper().findAndRegisterModules());
    }

    @Test
    void storedImageCanBeReadBack() {
        RoomImageStore store = newStore();
        byte[] jpeg = {1, 2, 3, 4};

        store.put("salotto", jpeg);

        RoomImageStore.Entry entry = store.find("salotto").orElseThrow();
        assertArrayEquals(jpeg, store.read(entry));
    }

    @Test
    void fileNameIsGeneratedByServerNotByClient() throws IOException {
        RoomImageStore store = newStore();
        store.put("salotto", new byte[]{9});

        String fileName = store.find("salotto").orElseThrow().fileName();
        assertTrue(fileName.matches("[0-9a-f-]{36}\\.jpg"), fileName);
        assertTrue(Files.exists(dataDir.resolve("room-images").resolve(fileName)));
    }

    @Test
    void metadataSurvivesRestart() {
        newStore().put("camera-da-letto", new byte[]{7, 7});

        // Nuova istanza sulla stessa cartella: simula il riavvio del backend.
        RoomImageStore restarted = newStore();

        Optional<RoomImageStore.Entry> entry = restarted.find("camera-da-letto");
        assertTrue(entry.isPresent());
        assertArrayEquals(new byte[]{7, 7}, restarted.read(entry.get()));
    }

    @Test
    void replacingRemovesThePreviousFile() throws IOException {
        RoomImageStore store = newStore();
        store.put("cucina", new byte[]{1});
        String previous = store.find("cucina").orElseThrow().fileName();

        store.put("cucina", new byte[]{2});

        assertFalse(Files.exists(dataDir.resolve("room-images").resolve(previous)));
        assertArrayEquals(new byte[]{2}, store.read(store.find("cucina").orElseThrow()));
        assertEquals(2, countFiles()); // una foto + index.json
    }

    @Test
    void removeDeletesFileAndMetadata() throws IOException {
        RoomImageStore store = newStore();
        store.put("bagno-grande", new byte[]{5});

        store.remove("bagno-grande");

        assertTrue(store.find("bagno-grande").isEmpty());
        assertTrue(newStore().find("bagno-grande").isEmpty());
        assertEquals(1, countFiles()); // solo index.json
    }

    @Test
    void removingAnUnknownRoomIsANoOp() {
        newStore().remove("non-esiste");
    }

    private long countFiles() throws IOException {
        try (Stream<Path> files = Files.list(dataDir.resolve("room-images"))) {
            return files.count();
        }
    }
}
