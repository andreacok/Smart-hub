import type { Device } from '../../api/types';
import { useDeviceCommand } from '../../hooks/useDeviceCommand';
import { formatWatts } from '../../lib/format';
import { DeviceCard } from './DeviceCard';

/** Presa smart o interruttore generico. */
export function SwitchCard({ device }: { device: Device }) {
  const { mutate } = useDeviceCommand();
  const on = device.state.power === 'ON';
  const canToggle = device.capabilities.includes('POWER');
  return (
    <DeviceCard
      device={device}
      tone={on && device.online ? 'active' : 'default'}
      {...(canToggle
        ? {
            onToggle: () => mutate({ device, command: { command: on ? 'TURN_OFF' : 'TURN_ON' } }),
            toggleLabel: `${device.name}: ${on ? 'spegni' : 'accendi'}`,
            pressed: on,
          }
        : {})}
      aside={on && device.state.powerUsage !== undefined ? formatWatts(device.state.powerUsage) : undefined}
      status={on ? 'Accesa' : 'Spenta'}
    />
  );
}
