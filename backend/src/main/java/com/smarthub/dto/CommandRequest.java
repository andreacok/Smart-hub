package com.smarthub.dto;

import com.smarthub.model.CommandType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Body di POST /api/devices/{id}/commands.
 * Esempi: {"command":"TURN_ON"}, {"command":"SET_BRIGHTNESS","value":70},
 * {"command":"SET_COLOR","value":{"hue":200,"saturation":80}}.
 */
public record CommandRequest(
        @NotNull
        @Schema(description = "Comando normalizzato", example = "SET_BRIGHTNESS")
        CommandType command,
        @Schema(description = "Valore del comando: numero (luminosità, posizione, gradi, kelvin), "
                + "stringa (modalità termostato) o oggetto {hue, saturation} per SET_COLOR. "
                + "Assente per TURN_ON, TURN_OFF, OPEN, CLOSE, STOP.", example = "70")
        Object value
) {
}
