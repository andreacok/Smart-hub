package com.smarthub.smartthings.api;

import java.util.List;

public record StComponent(String id, List<StCapabilityRef> capabilities, List<StCategory> categories) {

    public boolean hasCapability(String capabilityId) {
        return capabilities != null && capabilities.stream().anyMatch(c -> capabilityId.equals(c.id()));
    }
}
