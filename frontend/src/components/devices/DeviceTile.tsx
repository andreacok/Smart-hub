import type { Device } from '../../api/types';
import { GenericDeviceCard } from './GenericDeviceCard';
import { LightCard } from './LightCard';
import { SensorCard } from './SensorCard';
import { ShutterCard } from './ShutterCard';
import { SwitchCard } from './SwitchCard';
import { ThermostatCard } from './ThermostatCard';

/** Sceglie la card giusta in base al tipo normalizzato del dispositivo. */
export function DeviceTile({ device }: { device: Device }) {
  switch (device.type) {
    case 'LIGHT':
      return <LightCard device={device} />;
    case 'SWITCH':
      return <SwitchCard device={device} />;
    case 'SHUTTER':
      return <ShutterCard device={device} />;
    case 'THERMOSTAT':
      return <ThermostatCard device={device} />;
    case 'SENSOR':
      return <SensorCard device={device} />;
    default:
      return <GenericDeviceCard device={device} />;
  }
}
