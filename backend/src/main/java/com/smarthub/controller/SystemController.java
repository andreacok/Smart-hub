package com.smarthub.controller;

import com.smarthub.dto.SystemStatusDto;
import com.smarthub.service.SystemService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "Sistema")
@RequestMapping("/api/system")
public class SystemController {

    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    /** Modalità (mock/live) e stato della connessione con SmartThings. */
    @Operation(summary = "Modalità (mock/live) e stato della connessione SmartThings")
    @GetMapping("/status")
    public SystemStatusDto status() {
        return systemService.status();
    }
}
