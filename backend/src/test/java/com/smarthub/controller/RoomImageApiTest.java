package com.smarthub.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Flusso completo della fotografia personalizzata via HTTP, in mock mode con cartella dati temporanea. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "smartthings.mode=mock",
        "smartthings.mock.latency=0ms",
        "smartthings.command-refresh-delay=0ms"
})
class RoomImageApiTest {

    @TempDir
    static Path dataDir;

    @DynamicPropertySource
    static void dataDirectory(DynamicPropertyRegistry registry) {
        registry.add("smarthub.data-dir", () -> dataDir.toString());
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void uploadThenReadThenReplaceThenReset() throws Exception {
        MvcResult upload = mvc.perform(multipart("/api/rooms/salotto/image").file(jpeg("prima.jpg", 800, 600)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomId").value("salotto"))
                .andExpect(jsonPath("$.customImage").value(true))
                .andExpect(jsonPath("$.imageUrl", containsString("/api/rooms/salotto/image/content?v=")))
                .andReturn();
        String firstUrl = com.jayway.jsonpath.JsonPath.read(upload.getResponse().getContentAsString(), "$.imageUrl");

        // Il DTO stanza espone la stessa foto.
        mvc.perform(get("/api/rooms/salotto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.room.customImage").value(true))
                .andExpect(jsonPath("$.room.imageUrl").value(firstUrl));

        // Contenuto servito come JPEG con cache versionata.
        mvc.perform(get(firstUrl))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/jpeg"))
                .andExpect(header().string("Cache-Control", containsString("max-age=86400")));

        Thread.sleep(5); // garantisce un istante diverso per la nuova versione
        MvcResult replace = mvc.perform(multipart("/api/rooms/salotto/image").file(jpeg("dopo.jpg", 500, 500)))
                .andExpect(status().isOk())
                .andReturn();
        String secondUrl = com.jayway.jsonpath.JsonPath.read(replace.getResponse().getContentAsString(), "$.imageUrl");
        assertNotEquals(firstUrl, secondUrl, "la sostituzione deve cambiare l'URL (cache busting)");

        mvc.perform(delete("/api/rooms/salotto/image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customImage").value(false))
                .andExpect(jsonPath("$.imageUrl").doesNotExist());

        mvc.perform(get("/api/rooms/salotto/image/content"))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/rooms/salotto"))
                .andExpect(jsonPath("$.room.customImage").value(false))
                .andExpect(jsonPath("$.room.imageUrl").doesNotExist());
    }

    @Test
    void unknownRoomIsNotFound() throws Exception {
        mvc.perform(multipart("/api/rooms/stanza-inesistente/image").file(jpeg("a.jpg", 100, 100)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SMART_HUB_NOT_FOUND"));
    }

    @Test
    void invalidRoomIdIsBadRequest() throws Exception {
        mvc.perform(multipart("/api/rooms/bad%20id/image").file(jpeg("a.jpg", 100, 100)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SMART_HUB_VALIDATION_ERROR"));
    }

    @Test
    void unsupportedFormatIsRejected() throws Exception {
        MockMultipartFile text = new MockMultipartFile("file", "foto.jpg", "image/jpeg",
                "questo non è un'immagine".getBytes());
        mvc.perform(multipart("/api/rooms/cucina/image").file(text))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_FORMAT"));
    }

    @Test
    void tooLargeFileIsRejectedWith413() throws Exception {
        MockMultipartFile huge = new MockMultipartFile("file", "enorme.jpg", "image/jpeg",
                new byte[10 * 1024 * 1024 + 1]);
        mvc.perform(multipart("/api/rooms/cucina/image").file(huge))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("FILE_TOO_LARGE"));
    }

    @Test
    void missingFileFieldIsBadRequest() throws Exception {
        mvc.perform(multipart("/api/rooms/cucina/image"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void resetOfRoomWithoutCustomImageIsIdempotent() throws Exception {
        mvc.perform(delete("/api/rooms/bagno-piccolo/image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customImage").value(false));
    }

    private static MockMultipartFile jpeg(String name, int width, int height) throws IOException {
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", out);
        return new MockMultipartFile("file", name, MediaType.IMAGE_JPEG_VALUE, out.toByteArray());
    }
}
