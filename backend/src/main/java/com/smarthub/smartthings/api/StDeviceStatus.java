package com.smarthub.smartthings.api;

import java.util.Map;
import java.util.Optional;

/**
 * Risposta di GET /devices/{deviceId}/status:
 * components -> capability -> attributo -> { value, unit }.
 */
public record StDeviceStatus(Map<String, Map<String, Map<String, StAttribute>>> components) {

    public Optional<StAttribute> attribute(String capability, String attribute) {
        return attribute(StDevice.MAIN, capability, attribute);
    }

    public Optional<StAttribute> attribute(String component, String capability, String attribute) {
        if (components == null) {
            return Optional.empty();
        }
        Map<String, Map<String, StAttribute>> comp = components.get(component);
        if (comp == null && !components.isEmpty()) {
            comp = components.values().iterator().next();
        }
        if (comp == null) {
            return Optional.empty();
        }
        Map<String, StAttribute> cap = comp.get(capability);
        if (cap == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(cap.get(attribute)).filter(a -> a.value() != null);
    }
}
