import type { Device, Home, RoomDetail } from '../api/types';

export function light(overrides: Partial<Device> = {}): Device {
  return {
    id: 'soggiorno-lampadario',
    name: 'Lampadario',
    roomId: 'soggiorno',
    roomName: 'Soggiorno',
    type: 'LIGHT',
    online: true,
    state: { power: 'OFF', brightness: 60 },
    capabilities: ['POWER', 'DIMMER'],
    ...overrides,
  };
}

export function roomDetail(devices: Device[]): RoomDetail {
  return {
    room: {
      id: 'soggiorno',
      name: 'Soggiorno',
      deviceCount: devices.length,
      onlineCount: devices.length,
      activeCount: 0,
      lightsOn: 0,
      lightsTotal: devices.filter((d) => d.type === 'LIGHT').length,
      temperature: 22.4,
      shutterCount: 0,
      openContacts: 0,
      motion: false,
      alertCount: 0,
    },
    devices,
    alerts: [],
  };
}

export function home(): Home {
  return {
    location: { id: 'loc', name: 'Casa Demo' },
    mode: 'mock',
    summary: {
      devicesTotal: 1,
      devicesOnline: 1,
      activeDevices: 0,
      lightsOn: 0,
      lightsTotal: 1,
      averageTemperature: 22.4,
      openShutters: 0,
      openContacts: 0,
      alertCount: 0,
    },
    rooms: [roomDetail([light()]).room],
    alerts: [],
    scenes: [{ id: 'scene-film', name: 'Film', icon: 'movie' }],
    activeDevices: [],
    updatedAt: new Date().toISOString(),
  };
}
