import type { Device, DeviceType } from '../api/types';

export interface DeviceGroup {
  key: string;
  title: string;
  devices: Device[];
}

const GROUPS: { key: string; title: string; types: DeviceType[] }[] = [
  { key: 'lights', title: 'Luci', types: ['LIGHT'] },
  { key: 'shutters', title: 'Tapparelle', types: ['SHUTTER'] },
  { key: 'climate', title: 'Clima', types: ['THERMOSTAT'] },
  { key: 'switches', title: 'Prese e interruttori', types: ['SWITCH'] },
  { key: 'sensors', title: 'Sensori', types: ['SENSOR'] },
  { key: 'cameras', title: 'Videocamere', types: ['CAMERA'] },
  { key: 'other', title: 'Altro', types: ['OTHER'] },
];

/** Raggruppa i dispositivi di una stanza per tipo, nell'ordine in cui vengono mostrati. */
export function groupDevices(devices: Device[]): DeviceGroup[] {
  return GROUPS.map((g) => ({
    key: g.key,
    title: g.title,
    devices: devices.filter((d) => g.types.includes(d.type)),
  })).filter((g) => g.devices.length > 0);
}
