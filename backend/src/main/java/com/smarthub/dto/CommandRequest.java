package com.smarthub.dto;

import com.smarthub.model.CommandType;
import jakarta.validation.constraints.NotNull;

/**
 * Body di POST /api/devices/{id}/commands.
 * Esempi: {"command":"TURN_ON"}, {"command":"SET_BRIGHTNESS","value":70},
 * {"command":"SET_COLOR","value":{"hue":200,"saturation":80}}.
 */
public record CommandRequest(@NotNull CommandType command, Object value) {
}
