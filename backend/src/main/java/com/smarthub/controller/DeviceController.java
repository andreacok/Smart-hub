package com.smarthub.controller;

import com.smarthub.dto.CommandRequest;
import com.smarthub.dto.DeviceDto;
import com.smarthub.dto.DeviceStatusDto;
import com.smarthub.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@Tag(name = "Dispositivi")
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @Operation(summary = "Elenco dispositivi normalizzati (filtrabile per stanza)")
    @GetMapping
    public List<DeviceDto> devices(@RequestParam(required = false) String roomId) {
        return deviceService.list(roomId);
    }

    @Operation(summary = "Singolo dispositivo")
    @GetMapping("/{deviceId}")
    public DeviceDto device(@PathVariable String deviceId) {
        return deviceService.get(deviceId);
    }

    @Operation(summary = "Solo lo stato corrente del dispositivo")
    @GetMapping("/{deviceId}/status")
    public DeviceStatusDto status(@PathVariable String deviceId) {
        return deviceService.status(deviceId);
    }

    /** Invia un comando normalizzato e restituisce il dispositivo con lo stato atteso. */
    @Operation(
            summary = "Invia un comando al dispositivo",
            description = "Restituisce il dispositivo con lo stato atteso; lo stato reale viene riletto da SmartThings "
                    + "poco dopo. Errori: 400 comando non supportato, 404 dispositivo inesistente, "
                    + "409 dispositivo offline, 502 errore SmartThings.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = CommandRequest.class),
                            examples = {
                                    @ExampleObject(name = "Accendi", value = "{\"command\":\"TURN_ON\"}"),
                                    @ExampleObject(name = "Spegni", value = "{\"command\":\"TURN_OFF\"}"),
                                    @ExampleObject(name = "Luminosità", value = "{\"command\":\"SET_BRIGHTNESS\",\"value\":70}"),
                                    @ExampleObject(name = "Temperatura colore", value = "{\"command\":\"SET_COLOR_TEMPERATURE\",\"value\":2700}"),
                                    @ExampleObject(name = "Colore", value = "{\"command\":\"SET_COLOR\",\"value\":{\"hue\":200,\"saturation\":80}}"),
                                    @ExampleObject(name = "Apri tapparella", value = "{\"command\":\"OPEN\"}"),
                                    @ExampleObject(name = "Posizione tapparella", value = "{\"command\":\"SET_POSITION\",\"value\":60}"),
                                    @ExampleObject(name = "Stop tapparella", value = "{\"command\":\"STOP\"}"),
                                    @ExampleObject(name = "Temperatura desiderata", value = "{\"command\":\"SET_TARGET_TEMPERATURE\",\"value\":21.5}"),
                                    @ExampleObject(name = "Modalità termostato", value = "{\"command\":\"SET_THERMOSTAT_MODE\",\"value\":\"heat\"}")
                            })))
    @PostMapping("/{deviceId}/commands")
    public DeviceDto command(@PathVariable String deviceId, @Valid @RequestBody CommandRequest request) {
        return deviceService.execute(deviceId, request);
    }
}
