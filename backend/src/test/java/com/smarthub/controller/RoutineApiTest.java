package com.smarthub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Routine e automazioni in modalità mock: elenco normalizzato, ordinamento ed esecuzione reale. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "smartthings.mode=mock",
        "smartthings.mock.latency=0ms",
        "smartthings.command-refresh-delay=0ms"
})
class RoutineApiTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void listsManualRoutinesBeforeAutomationsSortedByName() throws Exception {
        mvc.perform(get("/api/routines"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(6)))
                .andExpect(jsonPath("$[0].name").value("Buonanotte"))
                .andExpect(jsonPath("$[0].type").value("MANUAL_ROUTINE"))
                .andExpect(jsonPath("$[0].manuallyExecutable").value(true))
                .andExpect(jsonPath("$[3].name").value("Film"))
                .andExpect(jsonPath("$[4].name").value("Chiudi tapparelle la sera"))
                .andExpect(jsonPath("$[4].type").value("AUTOMATION"))
                .andExpect(jsonPath("$[5].name").value("Luce corridoio al movimento"))
                .andExpect(jsonPath("$[5].type").value("AUTOMATION"));
    }

    @Test
    void executingSceneRunsItsActions() throws Exception {
        mvc.perform(post("/api/routines/scene-buonanotte/execute"))
                .andExpect(status().isNoContent());

        // "Buonanotte" spegne le luci: la lettura successiva deve riflettere l'esecuzione
        mvc.perform(get("/api/devices/cucina-luce"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state.power").value("OFF"));
    }

    @Test
    void executingAutomationRunsItsActions() throws Exception {
        mvc.perform(post("/api/routines/rule-luce-corridoio/execute"))
                .andExpect(status().isNoContent());

        mvc.perform(get("/api/devices/corridoio-luce"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state.power").value("ON"));
    }

    @Test
    void unknownRoutineIsNotFound() throws Exception {
        mvc.perform(post("/api/routines/does-not-exist/execute"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("SMART_HUB_NOT_FOUND"));
    }
}
