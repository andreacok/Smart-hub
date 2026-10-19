import { Link } from 'react-router-dom';
import type { RoomSummary } from '../../api/types';
import { formatPercent, formatTemperature, pluralize } from '../../lib/format';
import { LightMeter } from '../common/LightMeter';
import { StatusBadge } from '../common/StatusBadge';
import styles from './RoomCard.module.css';

/** Card grande di una stanza con lo stato aggregato; apre la pagina stanza. */
export function RoomCard({ room }: { room: RoomSummary }) {
  const details: string[] = [];
  if (room.lightsTotal > 0) {
    details.push(room.lightsOn === 0 ? 'Luci spente' : `${pluralize(room.lightsOn, 'luce accesa', 'luci accese')} su ${room.lightsTotal}`);
  }
  if (room.shutterPosition !== undefined) {
    details.push(
      room.shutterCount > 1
        ? `Tapparelle al ${formatPercent(room.shutterPosition)}`
        : `Tapparella al ${formatPercent(room.shutterPosition)}`,
    );
  }
  const otherActive = room.activeCount - room.lightsOn;
  if (otherActive > 0) details.push(pluralize(otherActive, 'presa accesa', 'prese accese'));
  if (room.humidity !== undefined) details.push(`Umidità ${formatPercent(room.humidity)}`);

  return (
    <Link to={`/rooms/${encodeURIComponent(room.id)}`} className={styles.card}>
      <div className={styles.top}>
        <h3 className={styles.name}>{room.name}</h3>
        {room.temperature !== undefined && <span className={styles.temp}>{formatTemperature(room.temperature)}</span>}
      </div>
      <LightMeter on={room.lightsOn} total={room.lightsTotal} />
      <ul className={styles.details}>
        {details.map((d) => (
          <li key={d}>{d}</li>
        ))}
        {details.length === 0 && <li>{pluralize(room.deviceCount, 'dispositivo', 'dispositivi')}</li>}
      </ul>
      {(room.alertCount > 0 || room.openContacts > 0 || room.motion) && (
        <div className={styles.badges}>
          {room.openContacts > 0 && <StatusBadge tone="warn">{room.openContacts === 1 ? 'Porta o finestra aperta' : `${room.openContacts} aperture`}</StatusBadge>}
          {room.motion && <StatusBadge tone="ok">Movimento</StatusBadge>}
          {room.alertCount > room.openContacts && (
            <StatusBadge tone="alert">{pluralize(room.alertCount - room.openContacts, 'avviso', 'avvisi')}</StatusBadge>
          )}
        </div>
      )}
    </Link>
  );
}
