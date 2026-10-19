import { describe, expect, it } from 'vitest';
import type { Device, DeviceFamily, DeviceType } from '../api/types';
import { deviceStatusText } from './format';

const device = (deviceType: DeviceType, deviceFamily: DeviceFamily, overrides: Partial<Device> = {}): Device => ({
  id: deviceType.toLowerCase(),
  name: deviceType,
  roomId: 'cucina',
  deviceType,
  deviceFamily,
  online: true,
  state: {},
  capabilities: ['TEMPERATURE'],
  ...overrides,
});

describe('deviceStatusText per elettrodomestici', () => {
  it('un frigorifero online mostra il tipo e la temperatura, mai una lettura da termostato', () => {
    const fridge = device('REFRIGERATOR', 'REFRIGERATION', { state: { temperature: 4, temperatureUnit: 'C' } });
    expect(deviceStatusText(fridge)).toBe('Frigorifero · 4,0°');
  });

  it('un forno senza stato mostra solo il tipo', () => {
    expect(deviceStatusText(device('OVEN', 'COOKING_APPLIANCE'))).toBe('Forno');
  });

  it('un dispositivo offline non mostra il tipo ma dichiara che non è raggiungibile', () => {
    const fridge = device('REFRIGERATOR', 'REFRIGERATION', { online: false });
    expect(deviceStatusText(fridge)).toBe('Non raggiungibile');
  });
});
