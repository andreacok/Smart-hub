package com.smarthub.smartthings.api;

import java.util.List;
import java.util.Optional;

/** Definizione di un dispositivo da GET /devices. */
public record StDevice(
        String deviceId,
        String name,
        String label,
        String locationId,
        String roomId,
        List<StComponent> components
) {
    public static final String MAIN = "main";

    public String displayName() {
        return label != null && !label.isBlank() ? label : name;
    }

    public Optional<StComponent> mainComponent() {
        if (components == null || components.isEmpty()) {
            return Optional.empty();
        }
        return components.stream().filter(c -> MAIN.equals(c.id())).findFirst()
                .or(() -> Optional.of(components.get(0)));
    }

    public boolean hasCapability(String capabilityId) {
        return mainComponent().map(c -> c.hasCapability(capabilityId)).orElse(false);
    }
}
