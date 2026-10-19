package com.smarthub.controller;

import com.smarthub.dto.ActionResultDto;
import com.smarthub.dto.RoomDetailDto;
import com.smarthub.dto.RoomSummaryDto;
import com.smarthub.service.RoomService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@Tag(name = "Stanze")
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @Operation(summary = "Elenco stanze con stato aggregato")
    @GetMapping
    public List<RoomSummaryDto> rooms() {
        return roomService.list();
    }

    @Operation(summary = "Dettaglio stanza con dispositivi e avvisi")
    @GetMapping("/{roomId}")
    public RoomDetailDto room(@PathVariable String roomId) {
        return roomService.get(roomId);
    }

    /** Spegne luci e prese accese della stanza. */
    @Operation(summary = "Spegne luci e prese accese della stanza")
    @PostMapping("/{roomId}/actions/all-off")
    public ActionResultDto allOff(@PathVariable String roomId) {
        return roomService.turnOffAll(roomId);
    }
}
