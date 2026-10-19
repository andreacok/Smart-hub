import type { Device } from '../../api/types';
import { useDeviceCommand } from '../../hooks/useDeviceCommand';
import { DeviceIcon } from '../devices/DeviceIcon';
import styles from './ActiveDevices.module.css';

/** "Acceso ora": tutto ciò che è acceso in casa, spegnibile con un tocco. */
export function ActiveDevices({ devices }: { devices: Device[] }) {
  const { mutate } = useDeviceCommand();
  if (devices.length === 0) {
    return <p className={styles.empty}>Tutto spento.</p>;
  }
  return (
    <ul className={styles.list}>
      {devices.map((device) => (
        <li key={device.id}>
          <button
            type="button"
            className={`${styles.chip} ${device.type === 'LIGHT' ? styles.light : ''}`}
            onClick={() => mutate({ device, command: { command: 'TURN_OFF' } })}
            aria-label={`Spegni ${device.name}${device.roomName ? ` in ${device.roomName}` : ''}`}
          >
            <DeviceIcon device={device} size={18} />
            <span className={styles.name}>{device.name}</span>
            {device.roomName && <span className={styles.room}>{device.roomName}</span>}
          </button>
        </li>
      ))}
    </ul>
  );
}
