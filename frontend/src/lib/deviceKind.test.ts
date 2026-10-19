import { describe, expect, it } from 'vitest';
import type { DeviceFamily, DeviceType } from '../api/types';
import { deviceKind, deviceTypeLabel } from './deviceKind';

const device = (deviceType: DeviceType, deviceFamily: DeviceFamily) => ({ deviceType, deviceFamily });

describe('deviceKind', () => {
  it.each<[DeviceType, DeviceFamily, string]>([
    ['LIGHT', 'LIGHTING', 'LIGHT'],
    ['SWITCH', 'POWER', 'SWITCH'],
    ['SMART_PLUG', 'POWER', 'SWITCH'],
    ['BLIND', 'COVERING', 'SHUTTER'],
    ['SHADE', 'COVERING', 'SHUTTER'],
    ['THERMOSTAT', 'CLIMATE', 'THERMOSTAT'],
    ['CAMERA', 'SECURITY', 'CAMERA'],
    ['TEMP_SENSOR', 'SENSOR', 'SENSOR'],
    ['GENERIC_SENSOR', 'SENSOR', 'SENSOR'],
  ])('%s -> %s', (type, family, kind) => {
    expect(deviceKind(device(type, family))).toBe(kind);
  });

  it('un forno, un frigorifero o un condizionatore non sono mai termostati', () => {
    expect(deviceKind(device('OVEN', 'COOKING_APPLIANCE'))).toBe('GENERIC');
    expect(deviceKind(device('REFRIGERATOR', 'REFRIGERATION'))).toBe('GENERIC');
    expect(deviceKind(device('AIR_CONDITIONER', 'CLIMATE'))).toBe('GENERIC');
  });

  it('tipi sconosciuti usano la card generica', () => {
    expect(deviceKind(device('UNKNOWN', 'GENERIC'))).toBe('GENERIC');
  });
});

describe('deviceTypeLabel', () => {
  it('usa il nome del tipo quando è noto', () => {
    expect(deviceTypeLabel(device('REFRIGERATOR', 'REFRIGERATION'))).toBe('Frigorifero');
    expect(deviceTypeLabel(device('OVEN', 'COOKING_APPLIANCE'))).toBe('Forno');
  });

  it('ripiega sulla famiglia per i tipi senza nome dedicato', () => {
    expect(deviceTypeLabel(device('PRINTER', 'GENERIC'))).toBe('Dispositivo');
    expect(deviceTypeLabel(device('HEATED_MATTRESS_PAD', 'CLIMATE'))).toBe('Clima');
  });
});
