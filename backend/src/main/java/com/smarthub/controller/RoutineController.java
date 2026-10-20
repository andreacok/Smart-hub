package com.smarthub.controller;

import com.smarthub.dto.RoutineDto;
import com.smarthub.service.RoutineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(name = "Routine")
@RequestMapping("/api/routines")
public class RoutineController {

    private final RoutineService routineService;

    public RoutineController(RoutineService routineService) {
        this.routineService = routineService;
    }

    @Operation(summary = "Elenco delle routine e automazioni della location")
    @GetMapping
    public List<RoutineDto> routines() {
        return routineService.list();
    }

    @Operation(summary = "Esegue una routine o un'automazione")
    @PostMapping("/{routineId}/execute")
    public ResponseEntity<Void> execute(@PathVariable String routineId) {
        routineService.execute(routineId);
        return ResponseEntity.noContent().build();
    }
}
