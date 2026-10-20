package com.smarthub.controller;

import com.smarthub.smartthings.api.SmartThingsApi;
import com.smarthub.smartthings.api.SmartThingsException;
import com.smarthub.smartthings.api.StLocation;
import com.smarthub.smartthings.error.SmartThingsErrorMapper;
import com.smarthub.smartthings.error.SmartThingsOperation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Se SmartThings non risponde al tab Routine, l'API deve restituire un errore classificato, non una lista vuota. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "smartthings.mode=mock",
        "smartthings.mock.latency=0ms",
        "smartthings.command-refresh-delay=0ms"
})
class RoutineErrorApiTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private SmartThingsApi api;

    @Test
    void smartThingsFailureOnRoutinesIsReturnedAsError() throws Exception {
        // La struttura della casa si carica con successo: il guasto riguarda solo le scene.
        when(api.listLocations()).thenReturn(List.of(new StLocation("loc-1", "Casa Test")));
        when(api.listRooms(any())).thenReturn(List.of());
        when(api.listDevices(any())).thenReturn(List.of());
        when(api.listScenes(any())).thenThrow(new SmartThingsException(
                SmartThingsErrorMapper.fromHttp(SmartThingsOperation.READ, null, 503,
                        SmartThingsErrorMapper.simulatedBody("Servizio non disponibile"), null)));

        mvc.perform(get("/api/routines"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SMARTTHINGS_UNAVAILABLE"))
                .andExpect(jsonPath("$.source").value("SMARTTHINGS"))
                .andExpect(jsonPath("$.retryable").value(true));
    }
}
