package com.smarthub.controller;

import com.smarthub.dto.SystemStatusDto;
import com.smarthub.service.SystemService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    private final SystemService systemService;

    public SystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    /** Modalità (mock/live) e stato della connessione con SmartThings. */
    @GetMapping("/status")
    public SystemStatusDto status() {
        return systemService.status();
    }
}
