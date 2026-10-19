import type { Device, OpenState } from '../api/types';
import { deviceKind, deviceTypeLabel } from './deviceKind';

const numberFormat = new Intl.NumberFormat('it-IT', { maximumFractionDigits: 1, minimumFractionDigits: 1 });

export function formatTemperature(value: number | undefined, unit = 'C'): string {
  if (value === undefined) return '–';
  return `${numberFormat.format(value)}°${unit === 'F' ? 'F' : ''}`;
}

export function formatPercent(value: number | undefined): string {
  return value === undefined ? '–' : `${Math.round(value)}%`;
}

export function formatWatts(value: number | undefined): string {
  if (value === undefined) return '–';
  return value >= 1000 ? `${numberFormat.format(value / 1000)} kW` : `${Math.round(value)} W`;
}

export function pluralize(count: number, singular: string, plural: string): string {
  return `${count} ${count === 1 ? singular : plural}`;
}

export function shutterLabel(state: OpenState | undefined, position: number | undefined): string {
  switch (state) {
    case 'OPENING':
      return 'In apertura';
    case 'CLOSING':
      return 'In chiusura';
    case 'OPEN':
      return 'Aperta';
    case 'CLOSED':
      return 'Chiusa';
    default:
      return position === undefined ? 'Stato sconosciuto' : `Aperta al ${Math.round(position)}%`;
  }
}

const MODE_LABELS: Record<string, string> = {
  heat: 'Caldo',
  cool: 'Freddo',
  auto: 'Auto',
  eco: 'Eco',
  off: 'Spento',
  emergencyheat: 'Emergenza',
  dryair: 'Deumidifica',
  fanonly: 'Ventola',
};

export function thermostatModeLabel(mode: string | undefined): string {
  if (!mode) return '–';
  return MODE_LABELS[mode.toLowerCase()] ?? mode;
}

export function greeting(date: Date = new Date()): string {
  const h = date.getHours();
  if (h >= 5 && h < 13) return 'Buongiorno';
  if (h >= 13 && h < 18) return 'Buon pomeriggio';
  return 'Buonasera';
}

export function relativeTime(iso: string | undefined, now: Date = new Date()): string {
  if (!iso) return '';
  const seconds = Math.round((now.getTime() - new Date(iso).getTime()) / 1000);
  if (seconds < 10) return 'adesso';
  if (seconds < 60) return `${seconds} s fa`;
  const minutes = Math.round(seconds / 60);
  if (minutes < 60) return `${minutes} min fa`;
  return new Date(iso).toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' });
}

/** Etichetta breve dello stato, mostrata sotto il nome del dispositivo. */
export function deviceStatusText(device: Device): string {
  const s = device.state;
  if (!device.online) return 'Non raggiungibile';
  switch (deviceKind(device)) {
    case 'LIGHT':
      if (s.power === 'OFF') return 'Spenta';
      return s.brightness !== undefined && device.capabilities.includes('DIMMER') ? `Accesa, ${s.brightness}%` : 'Accesa';
    case 'SWITCH':
      if (s.power === 'OFF') return 'Spenta';
      return s.powerUsage !== undefined ? `Accesa, ${formatWatts(s.powerUsage)}` : 'Accesa';
    case 'SHUTTER':
      return shutterLabel(s.openState, s.position);
    case 'THERMOSTAT':
      return `${thermostatModeLabel(s.thermostatMode)}, ${formatTemperature(s.targetTemperature)}`;
    case 'CAMERA':
      return s.power === 'OFF' ? 'Spenta' : 'Attiva';
    case 'GENERIC': {
      // Tipo sempre visibile: un frigorifero resta un frigorifero anche senza una card dedicata.
      const detail = genericDetail(device);
      return detail ? `${deviceTypeLabel(device)} · ${detail}` : deviceTypeLabel(device);
    }
    default:
      return genericDetail(device);
  }
}

/** Dettaglio dello stato di un dispositivo senza card dedicata: contatto, movimento, temperatura, accensione. */
function genericDetail(device: Device): string {
  const s = device.state;
  if (s.contact) return s.contact === 'OPEN' ? 'Aperta' : 'Chiusa';
  if (s.motion !== undefined) return s.motion ? 'Movimento rilevato' : 'Nessun movimento';
  if (s.temperature !== undefined) return formatTemperature(s.temperature, s.temperatureUnit);
  return s.power === 'ON' ? 'Acceso' : s.power === 'OFF' ? 'Spento' : '';
}
