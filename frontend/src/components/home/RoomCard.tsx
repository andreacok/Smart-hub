import type { CSSProperties } from 'react';
import { Link } from 'react-router-dom';
import type { RoomSummary } from '../../api/types';
import { resolveRoomPhoto } from '../../config/roomImages';
import { formatPercent, formatTemperature, pluralize } from '../../lib/format';
import { StatusBadge } from '../common/StatusBadge';
import styles from './RoomCard.module.css';

/** Massimo di informazioni utili mostrate su una card stanza: il resto si vede aprendo la stanza. */
const MAX_DETAILS = 2;

/** Card di una stanza: foto reale, nome, temperatura e stato aggregato sopra uno scrim. Apre la stanza. */
export function RoomCard({ room }: { room: RoomSummary }) {
  const photo = resolveRoomPhoto(room);
  const imageStyle = {
    '--pos-desktop': photo.position.desktop ?? 'center',
    '--pos-tablet': photo.position.tablet ?? photo.position.desktop ?? 'center',
    '--pos-mobile': photo.position.mobile ?? photo.position.tablet ?? 'center',
  } as CSSProperties;

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
      <img className={styles.photo} style={imageStyle} src={photo.src} alt="" />
      <div className={styles.body}>
        <div className={styles.head}>
          <h3 className={styles.name}>{room.name}</h3>
          {room.temperature !== undefined && <p className={styles.temp}>{formatTemperature(room.temperature)}</p>}
        </div>
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
      </div>
    </Link>
  );
}
