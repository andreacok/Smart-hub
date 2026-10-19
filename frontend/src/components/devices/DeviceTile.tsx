import type { Device } from '../../api/types';
import { deviceKind } from '../../lib/deviceKind';
import { GenericDeviceCard } from './GenericDeviceCard';
import { LightCard } from './LightCard';
import { SensorCard } from './SensorCard';
import { ShutterCard } from './ShutterCard';
import { SwitchCard } from './SwitchCard';
import { ThermostatCard } from './ThermostatCard';

/** Sceglie la card in base al comportamento del dispositivo. Tipi senza card dedicata usano la generica. */
export function DeviceTile({ device }: { device: Device }) {
  switch (deviceKind(device)) {
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
