package com.smarthub.dto;

import com.smarthub.model.Scene;

public record SceneDto(String id, String name, String icon) {

    public static SceneDto from(Scene scene) {
        return new SceneDto(scene.id(), scene.name(), scene.icon());
    }
}
