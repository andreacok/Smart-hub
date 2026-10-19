import type { Device } from '../../api/types';
import { formatPercent, formatTemperature } from '../../lib/format';
import { DeviceCard } from './DeviceCard';
import styles from './controls.module.css';

/** Sensore: apertura, movimento, temperatura/umidità, batteria. Solo lettura. */
export function SensorCard({ device }: { device: Device }) {
  const s = device.state;
  const readings: { label: string; value: string }[] = [];
  if (s.temperature !== undefined) readings.push({ label: 'Temperatura', value: formatTemperature(s.temperature, s.temperatureUnit) });
  if (s.humidity !== undefined) readings.push({ label: 'Umidità', value: formatPercent(s.humidity) });
  if (s.battery !== undefined) readings.push({ label: 'Batteria', value: formatPercent(s.battery) });

  const alerting = s.contact === 'OPEN' || s.motion === true;
  // Lo stato principale è il titolo grande al centro: sotto la card non ripetiamo il testo.
  let headline: string | undefined;
  if (s.contact !== undefined) headline = s.contact === 'OPEN' ? 'Aperta' : 'Chiusa';
  else if (s.motion !== undefined) headline = s.motion ? 'Movimento rilevato' : 'Nessun movimento';

  return (
    <DeviceCard
      device={device}
      tone={alerting && device.online ? 'active' : 'default'}
      {...(device.online ? { status: '' } : {})}
    >
      {headline && <p className={styles.headline}>{headline}</p>}
      {readings.length > 0 && (
        <dl className={styles.readings}>
          {readings.map((r) => (
            <div key={r.label}>
              <dt>{r.label}</dt>
              <dd>{r.value}</dd>
            </div>
          ))}
        </dl>
      )}
    </DeviceCard>
  );
}
