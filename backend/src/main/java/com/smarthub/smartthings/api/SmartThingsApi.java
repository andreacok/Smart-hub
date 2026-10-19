package com.smarthub.smartthings.api;

import java.util.List;

/**
 * Accesso alle API SmartThings al livello del modello "grezzo".
 * Esistono due implementazioni: live (HTTP verso api.smartthings.com) e mock (casa demo in memoria).
 * Tutta la normalizzazione avviene a valle, quindi mock e live percorrono lo stesso codice.
 */
public interface SmartThingsApi {

    List<StLocation> listLocations();

    List<StRoom> listRooms(String locationId);

    List<StDevice> listDevices(String locationId);

    StDeviceStatus getDeviceStatus(String deviceId);

    StHealth getDeviceHealth(String deviceId);

    void executeCommands(String deviceId, List<StCommand> commands);

    List<StScene> listScenes(String locationId);

    void executeScene(String sceneId);
}
