package com.smarthub.smartthings;

import com.smarthub.dto.RoutineDto;
import com.smarthub.model.Routine;
import com.smarthub.model.RoutineType;
import com.smarthub.smartthings.api.StRule;
import com.smarthub.smartthings.api.StScene;
import com.smarthub.smartthings.mapping.RoutineMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Le scene diventano routine manuali, le regole diventano automazioni; entrambe sono eseguibili. */
class RoutineMapperTest {

    @Test
    void sceneMapsToManualRoutine() {
        Routine routine = RoutineMapper.fromScene(new StScene("scene-1", "Buonanotte", "moon", "loc-1"));

        assertEquals(new Routine("scene-1", "Buonanotte", RoutineType.MANUAL_ROUTINE, true), routine);
    }

    @Test
    void ruleMapsToAutomation() {
        Routine routine = RoutineMapper.fromRule(new StRule("rule-1", "Luce corridoio"));

        assertEquals(new Routine("rule-1", "Luce corridoio", RoutineType.AUTOMATION, true), routine);
    }

    @Test
    void dtoKeepsNormalizedFields() {
        RoutineDto dto = RoutineDto.from(new Routine("rule-1", "Luce corridoio", RoutineType.AUTOMATION, true));

        assertEquals("rule-1", dto.id());
        assertEquals("Luce corridoio", dto.name());
        assertEquals(RoutineType.AUTOMATION, dto.type());
        assertTrue(dto.manuallyExecutable());
    }
}
