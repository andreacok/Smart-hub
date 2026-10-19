package com.smarthub.dto;

import com.smarthub.model.Location;

public record LocationDto(String id, String name) {

    public static LocationDto from(Location location) {
        return location == null ? null : new LocationDto(location.id(), location.name());
    }
}
