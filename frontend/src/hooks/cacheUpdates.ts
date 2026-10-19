import type { Device, Home, RoomDetail, RoomSummary } from '../api/types';
import { isActive } from '../lib/predict';

/** Sostituisce un dispositivo nel dettaglio stanza. */
export function replaceDeviceInRoom(room: RoomDetail | undefined, device: Device): RoomDetail | undefined {
  if (!room || !room.devices.some((d) => d.id === device.id)) return room;
  const devices = room.devices.map((d) => (d.id === device.id ? device : d));
  return { ...room, devices, room: adjustRoomSummary(room.room, room.devices, devices) };
}

/** Aggiorna gli aggregati della home quando cambia lo stato di un dispositivo. */
export function replaceDeviceInHome(home: Home | undefined, before: Device, after: Device): Home | undefined {
  if (!home) return home;
  const wasActive = isActive(before);
  const nowActive = isActive(after);
  const wasLightOn = before.type === 'LIGHT' && before.online && before.state.power === 'ON';
  const nowLightOn = after.type === 'LIGHT' && after.online && after.state.power === 'ON';
  const activeDelta = Number(nowActive) - Number(wasActive);
  const lightDelta = Number(nowLightOn) - Number(wasLightOn);

  const activeDevices = home.activeDevices.filter((d) => d.id !== after.id);
  if (nowActive) activeDevices.push(after);

  return {
    ...home,
    activeDevices,
    summary: {
      ...home.summary,
      activeDevices: home.summary.activeDevices + activeDelta,
      lightsOn: home.summary.lightsOn + lightDelta,
    },
    rooms: home.rooms.map((r) =>
      r.id === after.roomId
        ? { ...r, activeCount: r.activeCount + activeDelta, lightsOn: r.lightsOn + lightDelta }
        : r,
    ),
  };
}

function adjustRoomSummary(summary: RoomSummary, before: Device[], after: Device[]): RoomSummary {
  const lightsOn = (list: Device[]) =>
    list.filter((d) => d.type === 'LIGHT' && d.online && d.state.power === 'ON').length;
  const shutters = after.filter((d) => d.type === 'SHUTTER' && d.state.position !== undefined);
  const shutterPosition = shutters.length
    ? Math.round(shutters.reduce((sum, d) => sum + (d.state.position ?? 0), 0) / shutters.length)
    : summary.shutterPosition;
  return {
    ...summary,
    lightsOn: summary.lightsOn - lightsOn(before) + lightsOn(after),
    activeCount: summary.activeCount - before.filter(isActive).length + after.filter(isActive).length,
    ...(shutterPosition !== undefined ? { shutterPosition } : {}),
  };
}
