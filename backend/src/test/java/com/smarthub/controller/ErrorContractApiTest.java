package com.smarthub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Contratto degli errori REST: codice applicativo, sorgente, correlation ID, nessun HTML o stack trace. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "smartthings.mode=mock",
        "smartthings.mock.latency=0ms",
        "smartthings.command-refresh-delay=0ms"
})
class ErrorContractApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void errorResponseHasStableFieldsAndCorrelationId() throws Exception {
        mvc.perform(get("/api/devices/does-not-exist").header("X-Request-Id", "client-req-0001"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Request-Id", "client-req-0001"))
                .andExpect(jsonPath("$.code").value("SMART_HUB_NOT_FOUND"))
                .andExpect(jsonPath("$.source").value("SMART_HUB"))
                .andExpect(jsonPath("$.retryable").value(false))
                .andExpect(jsonPath("$.correlationId").value("client-req-0001"))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.path").value("/api/devices/does-not-exist"))
                .andExpect(jsonPath("$.timestamp").isNotEmpty());
    }

    @Test
    void invalidIncomingCorrelationIdIsReplacedByAGeneratedOne() throws Exception {
        var result = mvc.perform(get("/api/devices/does-not-exist").header("X-Request-Id", "bad id with spaces!!"))
                .andExpect(status().isNotFound())
                .andReturn();
        String generated = result.getResponse().getHeader("X-Request-Id");
        assertNotEquals("bad id with spaces!!", generated);
        assertTrue(generated != null && generated.length() >= 8);
    }

    @Test
    void smartThingsErrorsCarrySourceAndRetryFlag() throws Exception {
        // Device offline nella demo: SmartThings non è coinvolto nel calcolo, ma il codice è quello applicativo.
        mvc.perform(post("/api/devices/corridoio-videocamera/commands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"command\":\"TURN_OFF\"}"))
                .andExpect(status().isFailedDependency())
                .andExpect(jsonPath("$.code").value("SMARTTHINGS_DEVICE_OFFLINE"))
                .andExpect(jsonPath("$.source").value("SMARTTHINGS"))
                .andExpect(jsonPath("$.retryable").value(true))
                .andExpect(jsonPath("$.message").value("Il dispositivo non è raggiungibile tramite SmartThings."));
    }

    @Test
    void malformedBodyNeverReturnsSpringHtmlOrStackTrace() throws Exception {
        var result = mvc.perform(post("/api/devices/cucina-presa-caffe/commands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SMART_HUB_VALIDATION_ERROR"))
                .andReturn();
        String body = result.getResponse().getContentAsString();
        assertFalse(body.contains("<html"), body);
        assertFalse(body.contains("Exception"), body);
        assertFalse(body.contains("at com.smarthub"), body);
    }
}
