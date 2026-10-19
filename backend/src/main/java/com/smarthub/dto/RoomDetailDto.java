package com.smarthub.dto;

import java.util.List;

/** Pagina stanza: riepilogo + dispositivi. */
public record RoomDetailDto(RoomSummaryDto room, List<DeviceDto> devices, List<AlertDto> alerts) {
}
