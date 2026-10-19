import type { Device } from '../../api/types';
import { DeviceTile } from '../devices/DeviceTile';
import styles from './DeviceSection.module.css';

/** Gruppo di dispositivi dello stesso tipo nella pagina stanza. */
export function DeviceSection({ title, devices }: { title: string; devices: Device[] }) {
  if (devices.length === 0) return null;
  return (
    <section className={styles.section} aria-label={title}>
      <h2 className={styles.title}>
        {title}
        <span className={styles.count}>{devices.length}</span>
      </h2>
      <div className={styles.grid}>
        {devices.map((device) => (
          <DeviceTile key={device.id} device={device} />
        ))}
      </div>
    </section>
  );
}
