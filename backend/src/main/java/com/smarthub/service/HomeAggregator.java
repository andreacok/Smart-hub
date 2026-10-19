package com.smarthub.service;

import com.smarthub.dto.AlertDto;
import com.smarthub.dto.HomeSummaryDto;
import com.smarthub.dto.RoomSummaryDto;
import com.smarthub.model.Capability;
import com.smarthub.model.ContactState;
import com.smarthub.model.Device;
import com.smarthub.model.DeviceType;
import com.smarthub.model.OpenState;
import com.smarthub.model.Room;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.OptionalDouble;

/** Calcoli aggregati (riepiloghi casa/stanza, avvisi). Funzioni pure, senza dipendenze da Spring. */
public final class HomeAggregator {

    public static final String UNASSIGNED_ROOM_ID = "unassigned";
    public static final String UNASSIGNED_ROOM_NAME = "Altri dispositivi";
    static final int LOW_BATTERY_THRESHOLD = 20;

    private HomeAggregator() {
    }

    /** Un dispositivo è "attivo" se acceso (luci, prese, interruttori). */
    public static boolean isActive(Device d) {
        return d.online() && d.has(Capability.POWER) && d.isOn()
                && (d.type() == DeviceType.LIGHT || d.type() == DeviceType.SWITCH);
    }

    public static RoomSummaryDto roomSummary(Room room, Collection<Device> devices, List<AlertDto> alerts) {
        List<Device> lights = devices.stream().filter(d -> d.type() == DeviceType.LIGHT).toList();
        List<Device> shutters = devices.stream()
                .filter(d -> d.type() == DeviceType.SHUTTER && d.state().position() != null).toList();
        Integer shutterPosition = shutters.isEmpty() ? null
                : (int) Math.round(shutters.stream().mapToInt(d -> d.state().position()).average().orElse(0));
        int roomAlerts = (int) alerts.stream().filter(a -> room.id().equals(a.roomId())).count();
        return new RoomSummaryDto(
                room.id(),
                room.name(),
                devices.size(),
                (int) devices.stream().filter(Device::online).count(),
                (int) devices.stream().filter(HomeAggregator::isActive).count(),
                (int) lights.stream().filter(d -> d.online() && d.isOn()).count(),
                lights.size(),
                averageTemperature(devices),
                averageHumidity(devices),
                shutterPosition,
                shutters.size(),
                (int) devices.stream().filter(d -> d.state().contact() == ContactState.OPEN).count(),
                devices.stream().anyMatch(d -> Boolean.TRUE.equals(d.state().motion())),
                roomAlerts
        );
    }

    public static HomeSummaryDto homeSummary(Collection<Device> devices, List<AlertDto> alerts) {
        List<Device> lights = devices.stream().filter(d -> d.type() == DeviceType.LIGHT).toList();
        double power = devices.stream().map(d -> d.state().powerUsage()).filter(Objects::nonNull)
                .mapToDouble(Double::doubleValue).sum();
        boolean hasPowerMeters = devices.stream().anyMatch(d -> d.has(Capability.POWER_METER));
        return new HomeSummaryDto(
                devices.size(),
                (int) devices.stream().filter(Device::online).count(),
                (int) devices.stream().filter(HomeAggregator::isActive).count(),
                (int) lights.stream().filter(d -> d.online() && d.isOn()).count(),
                lights.size(),
                averageTemperature(devices),
                averageHumidity(devices),
                (int) devices.stream().filter(d -> d.type() == DeviceType.SHUTTER)
                        .filter(d -> d.state().openState() != OpenState.CLOSED && d.state().openState() != null)
                        .count(),
                (int) devices.stream().filter(d -> d.state().contact() == ContactState.OPEN).count(),
                hasPowerMeters ? Math.round(power * 10) / 10.0 : null,
                alerts.size()
        );
    }

    public static List<AlertDto> alerts(Collection<Device> devices) {
        List<AlertDto> result = new ArrayList<>();
        for (Device d : devices) {
            if (!d.online()) {
                result.add(new AlertDto("offline-" + d.id(), AlertDto.Severity.WARNING, AlertDto.Kind.OFFLINE,
                        d.name() + " non raggiungibile", d.id(), d.roomId()));
                continue;
            }
            if (d.state().contact() == ContactState.OPEN) {
                result.add(new AlertDto("contact-" + d.id(), AlertDto.Severity.INFO, AlertDto.Kind.OPEN_CONTACT,
                        d.name() + " aperta", d.id(), d.roomId()));
            }
            Integer battery = d.state().battery();
            if (battery != null && battery < LOW_BATTERY_THRESHOLD) {
                result.add(new AlertDto("battery-" + d.id(),
                        battery < 10 ? AlertDto.Severity.CRITICAL : AlertDto.Severity.WARNING,
                        AlertDto.Kind.LOW_BATTERY, d.name() + ": batteria al " + battery + "%", d.id(), d.roomId()));
            }
        }
        return result;
    }

    /**
     * Temperatura media della stanza: privilegia i sensori dedicati,
     * altrimenti usa la temperatura misurata dal termostato.
     */
    static Double averageTemperature(Collection<Device> devices) {
        List<Device> withTemp = devices.stream()
                .filter(d -> d.online() && d.state().temperature() != null).toList();
        List<Device> sensors = withTemp.stream().filter(d -> d.type() != DeviceType.THERMOSTAT).toList();
        OptionalDouble avg = (sensors.isEmpty() ? withTemp : sensors).stream()
                .mapToDouble(d -> d.state().temperature()).average();
        return avg.isPresent() ? Math.round(avg.getAsDouble() * 10) / 10.0 : null;
    }

    static Double averageHumidity(Collection<Device> devices) {
        OptionalDouble avg = devices.stream().filter(d -> d.online() && d.state().humidity() != null)
                .mapToDouble(d -> d.state().humidity()).average();
        return avg.isPresent() ? (double) Math.round(avg.getAsDouble()) : null;
    }
}
