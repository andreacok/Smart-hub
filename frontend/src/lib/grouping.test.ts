import { describe, expect, it } from 'vitest';
import type { Device, DeviceFamily, DeviceType } from '../api/types';
import { groupDevices } from './grouping';

const make = (id: string, deviceType: DeviceType, deviceFamily: DeviceFamily): Device => ({
  id,
  name: id,
  roomId: 'cucina',
  deviceType,
  deviceFamily,
  online: true,
  state: {},
  capabilities: [],
});

describe('groupDevices', () => {
  it('un frigorifero e un forno non finiscono mai nel gruppo Clima', () => {
    const groups = groupDevices([
      make('frigo', 'REFRIGERATOR', 'REFRIGERATION'),
      make('forno', 'OVEN', 'COOKING_APPLIANCE'),
      make('termostato', 'THERMOSTAT', 'CLIMATE'),
    ]);
    const byKey = Object.fromEntries(groups.map((g) => [g.key, g.devices.map((d) => d.id)]));
    expect(byKey.climate).toEqual(['termostato']);
    expect(byKey.appliances).toEqual(['frigo', 'forno']);
  });

  it('ogni dispositivo compare in un solo gruppo', () => {
    const devices = [
      make('luce', 'LIGHT', 'LIGHTING'),
      make('presa', 'SMART_PLUG', 'POWER'),
      make('tenda', 'BLIND', 'COVERING'),
      make('condizionatore', 'AIR_CONDITIONER', 'CLIMATE'),
      make('sconosciuto', 'UNKNOWN', 'GENERIC'),
    ];
    const grouped = groupDevices(devices).flatMap((g) => g.devices.map((d) => d.id));
    expect(grouped.sort()).toEqual(devices.map((d) => d.id).sort());
  });
});
