package com.smarthub.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Test end-to-end delle API REST in modalità mock. */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "smartthings.mode=mock",
        "smartthings.mock.latency=0ms",
        "smartthings.command-refresh-delay=0ms"
})
class ApiIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void homeEndpointReturnsDashboardData() throws Exception {
        mvc.perform(get("/api/home"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.location.name").value("Casa Demo"))
                .andExpect(jsonPath("$.mode").value("mock"))
                .andExpect(jsonPath("$.rooms", hasSize(8)))
                .andExpect(jsonPath("$.summary.devicesTotal").value(greaterThan(10)))
                .andExpect(jsonPath("$.scenes", hasSize(4)));
    }

    @Test
    void roomDetailContainsDevices() throws Exception {
        mvc.perform(get("/api/rooms/salotto"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.room.name").value("Salotto"))
                .andExpect(jsonPath("$.devices[0].deviceType").value("LIGHT"));
    }

    @Test
    void commandUpdatesDeviceState() throws Exception {
        mvc.perform(post("/api/devices/camera-da-letto-luce/commands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"command\":\"SET_BRIGHTNESS\",\"value\":40}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state.power").value("ON"))
                .andExpect(jsonPath("$.state.brightness").value(40));

        mvc.perform(get("/api/devices/camera-da-letto-luce/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.state.brightness").value(40));
    }

    @Test
    void errorsAreMappedToJson() throws Exception {
        mvc.perform(get("/api/devices/does-not-exist"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mvc.perform(post("/api/devices/cucina-presa-caffe/commands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"command\":\"SET_BRIGHTNESS\",\"value\":40}"))
                .andExpect(status().isBadRequest());

        mvc.perform(post("/api/devices/corridoio-videocamera/commands")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"command\":\"TURN_OFF\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEVICE_OFFLINE"));
    }

    @Test
    void openApiDocumentationIsPublished() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Smart Hub API"))
                .andExpect(jsonPath("$.paths['/api/devices/{deviceId}/commands']").exists());
    }

    @Test
    void systemStatusReportsMockMode() throws Exception {
        mvc.perform(get("/api/system/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mode").value("mock"))
                .andExpect(jsonPath("$.connected").value(true));
    }
}
