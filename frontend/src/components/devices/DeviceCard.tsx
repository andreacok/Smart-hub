import type { ReactNode } from 'react';
import type { Device } from '../../api/types';
import { deviceStatusText } from '../../lib/format';
import { StatusBadge } from '../common/StatusBadge';
import { DeviceIcon } from './DeviceIcon';
import styles from './DeviceCard.module.css';

export type CardTone = 'default' | 'lit' | 'active';

interface DeviceCardProps {
  device: Device;
  tone?: CardTone;
  /** Se presente, l'intestazione della card diventa un pulsante (es. accendi/spegni). */
  onToggle?: () => void;
  toggleLabel?: string;
  pressed?: boolean;
  status?: ReactNode;
  aside?: ReactNode;
  children?: ReactNode;
  wide?: boolean;
}

/** Contenitore comune delle card dispositivo: intestazione, stato, avvisi e area controlli. */
export function DeviceCard({
  device,
  tone = 'default',
  onToggle,
  toggleLabel,
  pressed,
  status,
  aside,
  children,
  wide = false,
}: DeviceCardProps) {
  const battery = device.state.battery;
  const statusText = status ?? deviceStatusText(device);
  const header = (
    <>
      <span className={styles.icon}>
        <DeviceIcon device={device} />
      </span>
      <span className={styles.text}>
        <span className={styles.name}>{device.name}</span>
        {statusText !== '' && <span className={styles.status}>{statusText}</span>}
      </span>
      {aside && <span className={styles.aside}>{aside}</span>}
    </>
  );

  return (
    <article
      className={`${styles.card} ${styles[tone]} ${wide ? styles.wide : ''} ${device.online ? '' : styles.offline}`}
      data-testid={`device-${device.id}`}
    >
      {onToggle ? (
        <button
          type="button"
          className={styles.header}
          onClick={onToggle}
          disabled={!device.online}
          aria-pressed={pressed}
          aria-label={toggleLabel ?? device.name}
        >
          {header}
        </button>
      ) : (
        <div className={styles.header}>{header}</div>
      )}

      {(!device.online || (battery !== undefined && battery < 20)) && (
        <div className={styles.badges}>
          {!device.online && <StatusBadge tone="alert">Offline</StatusBadge>}
          {battery !== undefined && battery < 20 && <StatusBadge tone="warn">Batteria {battery}%</StatusBadge>}
        </div>
      )}

      {children && device.online && <div className={styles.controls}>{children}</div>}
    </article>
  );
}
