package com.smarthub.dto;

import com.smarthub.model.Routine;
import com.smarthub.model.RoutineType;

/**
 * Routine per il frontend. {@code manuallyExecutable} indica se il pulsante "Esegui" può essere mostrato.
 * Lo stato dell'automazione non è incluso: le API SmartThings non lo espongono in modo affidabile.
 */
public record RoutineDto(String id, String name, RoutineType type, boolean manuallyExecutable) {

    public static RoutineDto from(Routine routine) {
        return new RoutineDto(routine.id(), routine.name(), routine.type(), routine.manuallyExecutable());
    }
}
