package com.smarthub.smartthings.mapping;

import com.smarthub.model.Routine;
import com.smarthub.model.RoutineType;
import com.smarthub.smartthings.api.StRule;
import com.smarthub.smartthings.api.StScene;

/**
 * Converte le entità grezze SmartThings in routine normalizzate.
 * Entrambe le API espongono un endpoint {@code /execute}, quindi la routine è eseguibile manualmente.
 */
public final class RoutineMapper {

    private RoutineMapper() {
    }

    public static Routine fromScene(StScene scene) {
        return new Routine(scene.sceneId(), scene.sceneName(), RoutineType.MANUAL_ROUTINE, true);
    }

    public static Routine fromRule(StRule rule) {
        return new Routine(rule.id(), rule.name(), RoutineType.AUTOMATION, true);
    }
}
