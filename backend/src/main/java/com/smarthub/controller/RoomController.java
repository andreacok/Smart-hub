package com.smarthub.controller;

import com.smarthub.dto.ActionResultDto;
import com.smarthub.dto.RoomDetailDto;
import com.smarthub.dto.RoomImageDto;
import com.smarthub.dto.RoomSummaryDto;
import com.smarthub.exception.NotFoundException;
import com.smarthub.exception.RoomImageException;
import com.smarthub.service.RoomImageProcessor;
import com.smarthub.service.RoomImageService;
import com.smarthub.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

@RestController
@Tag(name = "Stanze")
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final RoomImageService roomImageService;

    public RoomController(RoomService roomService, RoomImageService roomImageService) {
        this.roomService = roomService;
        this.roomImageService = roomImageService;
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

    @Operation(summary = "Carica la fotografia personalizzata della stanza (JPEG, PNG; WebP e HEIC convertiti nel browser)")
    @PostMapping(value = "/{roomId}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public RoomImageDto uploadImage(@PathVariable String roomId, @RequestParam("file") MultipartFile file) {
        if (file.getSize() > RoomImageProcessor.MAX_BYTES) {
            throw new RoomImageException(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                    "La foto supera il limite di 10 MB.");
        }
        try {
            return roomImageService.upload(roomId, file.getBytes());
        } catch (IOException e) {
            throw new RoomImageException(HttpStatus.BAD_REQUEST, "UPLOAD_FAILED",
                    "Caricamento non riuscito. Riprova con una foto diversa.");
        }
    }

    @Operation(summary = "Ripristina la fotografia predefinita della stanza")
    @DeleteMapping("/{roomId}/image")
    public RoomImageDto resetImage(@PathVariable String roomId) {
        return roomImageService.reset(roomId);
    }

    @Operation(summary = "Contenuto JPEG della fotografia personalizzata")
    @GetMapping("/{roomId}/image/content")
    public ResponseEntity<byte[]> imageContent(@PathVariable String roomId) {
        RoomImageService.StoredImage image = roomImageService.content(roomId)
                .orElseThrow(() -> new NotFoundException("La stanza non ha una foto personalizzata"));
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                // L'URL contiene la versione (?v=): quando cambia la foto cambia anche l'URL.
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePublic())
                .eTag("\"" + image.updatedAt().toEpochMilli() + "\"")
                .body(image.bytes());
    }
}
