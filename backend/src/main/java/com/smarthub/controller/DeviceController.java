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

import java.util.List;

@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @GetMapping
    public List<DeviceDto> devices(@RequestParam(required = false) String roomId) {
        return deviceService.list(roomId);
    }

    @GetMapping("/{deviceId}")
    public DeviceDto device(@PathVariable String deviceId) {
        return deviceService.get(deviceId);
    }

    @GetMapping("/{deviceId}/status")
    public DeviceStatusDto status(@PathVariable String deviceId) {
        return deviceService.status(deviceId);
    }

    /** Invia un comando normalizzato e restituisce il dispositivo con lo stato atteso. */
    @PostMapping("/{deviceId}/commands")
    public DeviceDto command(@PathVariable String deviceId, @Valid @RequestBody CommandRequest request) {
        return deviceService.execute(deviceId, request);
    }
}
