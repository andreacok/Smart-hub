import { Link } from 'react-router-dom';
import type { RoomSummary } from '../../api/types';
import { formatPercent, formatTemperature, pluralize } from '../../lib/format';
import { StatusBadge } from '../common/StatusBadge';
import styles from './RoomCard.module.css';

/** Massimo di informazioni utili mostrate su una card stanza: il resto si vede aprendo la stanza. */
const MAX_DETAILS = 3;

/** Card di una stanza: nome, temperatura in primo piano e poche info utili. Apre la pagina stanza. */
export function RoomCard({ room }: { room: RoomSummary }) {
  const details: string[] = [];
  if (room.lightsTotal > 0) {
    details.push(room.lightsOn === 0 ? 'Luce spenta' : pluralize(room.lightsOn, 'luce accesa', 'luci accese'));
  }
  if (room.shutterPosition !== undefined) {
    details.push(`Tapparella ${formatPercent(room.shutterPosition)}`);
  }
  const otherActive = room.activeCount - room.lightsOn;
  if (otherActive > 0) details.push(pluralize(otherActive, 'presa attiva', 'prese attive'));
  if (room.humidity !== undefined) details.push(`Umidità ${formatPercent(room.humidity)}`);

  const lit = room.lightsOn > 0;

  return (
    <Link to={`/rooms/${encodeURIComponent(room.id)}`} className={`${styles.card} ${lit ? styles.lit : ''}`}>
      <h3 className={styles.name}>{room.name}</h3>
      {room.temperature !== undefined && <p className={styles.temp}>{formatTemperature(room.temperature)}</p>}
      <ul className={styles.details}>
        {details.slice(0, MAX_DETAILS).map((d) => (
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
