package com.smarthub.dto;

import java.time.Instant;
import java.util.List;

/** Tutto ciò che serve alla home dashboard in una sola chiamata. */
public record HomeDto(
        LocationDto location,
        String ownerName,
        String mode,
        HomeSummaryDto summary,
        List<RoomSummaryDto> rooms,
        List<AlertDto> alerts,
        List<SceneDto> scenes,
        List<DeviceDto> activeDevices,
        Instant updatedAt
) {
}
