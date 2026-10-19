import type { Device } from '../api/types';
import { deviceKind, type DeviceKind } from './deviceKind';

export interface DeviceGroup {
  key: string;
  title: string;
  devices: Device[];
}

/** Famiglie di elettrodomestici: raggruppate insieme, senza mescolarle con il resto. */
const APPLIANCE_FAMILIES = new Set(['REFRIGERATION', 'COOKING_APPLIANCE', 'LAUNDRY', 'CLEANING']);

const GROUPS: { key: string; title: string; matches: (d: Device) => boolean }[] = [
  { key: 'lights', title: 'Luci', matches: kindIs('LIGHT') },
  { key: 'shutters', title: 'Tapparelle', matches: kindIs('SHUTTER') },
  { key: 'climate', title: 'Clima', matches: kindIs('THERMOSTAT') },
  { key: 'switches', title: 'Prese e interruttori', matches: kindIs('SWITCH') },
  { key: 'sensors', title: 'Sensori', matches: kindIs('SENSOR') },
  { key: 'cameras', title: 'Videocamere', matches: kindIs('CAMERA') },
  { key: 'appliances', title: 'Elettrodomestici', matches: (d) => APPLIANCE_FAMILIES.has(d.deviceFamily) },
  { key: 'other', title: 'Altro', matches: () => true },
];

function kindIs(kind: DeviceKind) {
  return (d: Device) => deviceKind(d) === kind;
}

/**
 * Raggruppa i dispositivi di una stanza, nell'ordine in cui vengono mostrati.
 * Ogni dispositivo finisce in un solo gruppo: il primo che lo riconosce.
 */
export function groupDevices(devices: Device[]): DeviceGroup[] {
  const remaining = [...devices];
  return GROUPS.map((g) => {
    const matched = remaining.filter(g.matches);
    matched.forEach((d) => remaining.splice(remaining.indexOf(d), 1));
    return { key: g.key, title: g.title, devices: matched };
  }).filter((g) => g.devices.length > 0);
}
