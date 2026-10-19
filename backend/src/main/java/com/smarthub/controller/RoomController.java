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

import java.util.List;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @GetMapping
    public List<RoomSummaryDto> rooms() {
        return roomService.list();
    }

    @GetMapping("/{roomId}")
    public RoomDetailDto room(@PathVariable String roomId) {
        return roomService.get(roomId);
    }

    /** Spegne luci e prese accese della stanza. */
    @PostMapping("/{roomId}/actions/all-off")
    public ActionResultDto allOff(@PathVariable String roomId) {
        return roomService.turnOffAll(roomId);
    }
}
