import { ChevronLeft, Power } from 'lucide-react';
import { Link } from 'react-router-dom';
import type { RoomSummary } from '../../api/types';
import { formatPercent, formatTemperature, pluralize } from '../../lib/format';
import { QuickAction } from '../common/QuickAction';
import styles from './RoomHeader.module.css';

interface RoomHeaderProps {
  room: RoomSummary;
  onAllOff: () => void;
  allOffBusy: boolean;
}

/** Intestazione della pagina stanza: nome, clima e azioni rapide. */
export function RoomHeader({ room, onAllOff, allOffBusy }: RoomHeaderProps) {
  const facts: string[] = [];
  if (room.lightsTotal > 0) {
    facts.push(room.lightsOn === 0 ? 'Luci spente' : pluralize(room.lightsOn, 'luce accesa', 'luci accese'));
  }
  if (room.humidity !== undefined) facts.push(`Umidità ${formatPercent(room.humidity)}`);
  facts.push(pluralize(room.deviceCount, 'dispositivo', 'dispositivi'));

  return (
    <header className={styles.header}>
      <Link to="/" className={styles.back} aria-label="Torna alla casa">
        <ChevronLeft size={28} aria-hidden="true" />
      </Link>
      <div className={styles.titleBlock}>
        <h1 className={styles.title}>{room.name}</h1>
        <p className={styles.facts}>{facts.join(', ')}</p>
      </div>
      {room.temperature !== undefined && (
        <p className={styles.temperature}>
          <span className="visually-hidden">Temperatura </span>
          {formatTemperature(room.temperature)}
        </p>
      )}
      <div className={styles.actions}>
        <QuickAction
          label="Spegni tutto"
          icon={<Power size={20} />}
          onClick={onAllOff}
          busy={allOffBusy}
          disabled={room.activeCount === 0}
        />
      </div>
    </header>
  );
}
