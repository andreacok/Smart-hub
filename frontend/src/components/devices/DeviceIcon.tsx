import {
  Blinds,
  Cctv,
  DoorClosed,
  DoorOpen,
  Droplets,
  Footprints,
  Lightbulb,
  Plug,
  Power,
  Thermometer,
} from 'lucide-react';
import type { Device } from '../../api/types';

/** Icona del dispositivo: una sola per tipo, utile al riconoscimento rapido sul tablet. */
export function DeviceIcon({ device, size = 22 }: { device: Device; size?: number }) {
  const props = { size, strokeWidth: 1.8, 'aria-hidden': true } as const;
  switch (device.type) {
    case 'LIGHT':
      return <Lightbulb {...props} />;
    case 'SWITCH':
      return <Plug {...props} />;
    case 'SHUTTER':
      return <Blinds {...props} />;
    case 'THERMOSTAT':
      return <Thermometer {...props} />;
    case 'CAMERA':
      return <Cctv {...props} />;
    case 'SENSOR':
      if (device.capabilities.includes('CONTACT')) {
        return device.state.contact === 'OPEN' ? <DoorOpen {...props} /> : <DoorClosed {...props} />;
      }
      if (device.capabilities.includes('MOTION')) return <Footprints {...props} />;
      if (device.capabilities.includes('HUMIDITY') && !device.capabilities.includes('TEMPERATURE')) {
        return <Droplets {...props} />;
      }
      return <Thermometer {...props} />;
    default:
      return <Power {...props} />;
  }
}
