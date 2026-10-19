package com.smarthub.dto;

/** Riepilogo generale della casa. */
public record HomeSummaryDto(
        int devicesTotal,
        int devicesOnline,
        int activeDevices,
        int lightsOn,
        int lightsTotal,
        Double averageTemperature,
        Double averageHumidity,
        int openShutters,
        int openContacts,
        Double powerUsage,
        int alertCount
) {
}
