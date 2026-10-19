import type { Device } from '../../api/types';
import { useDeviceCommand } from '../../hooks/useDeviceCommand';
import { DeviceCard } from './DeviceCard';

/** Videocamere e dispositivi non classificati: stato e, se possibile, accensione. */
export function GenericDeviceCard({ device }: { device: Device }) {
  const { mutate } = useDeviceCommand();
  const on = device.state.power === 'ON';
  if (!device.capabilities.includes('POWER')) {
    return <DeviceCard device={device} />;
  }
  return (
    <DeviceCard
      device={device}
      tone={on && device.online ? 'active' : 'default'}
      onToggle={() => mutate({ device, command: { command: on ? 'TURN_OFF' : 'TURN_ON' } })}
      toggleLabel={`${device.name}: ${on ? 'spegni' : 'accendi'}`}
      pressed={on}
    />
  );
}
