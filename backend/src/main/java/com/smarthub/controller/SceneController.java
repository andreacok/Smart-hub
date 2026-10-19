package com.smarthub.controller;

import com.smarthub.dto.SceneDto;
import com.smarthub.service.SceneService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@Tag(name = "Scene")
@RequestMapping("/api/scenes")
public class SceneController {

    private final SceneService sceneService;

    public SceneController(SceneService sceneService) {
        this.sceneService = sceneService;
    }

    @Operation(summary = "Elenco scene")
    @GetMapping
    public List<SceneDto> scenes() {
        return sceneService.list();
    }

    @Operation(summary = "Esegue una scena")
    @PostMapping("/{sceneId}/execute")
    public ResponseEntity<Void> execute(@PathVariable String sceneId) {
        sceneService.execute(sceneId);
        return ResponseEntity.noContent().build();
    }
}
