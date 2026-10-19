import { ChevronLeft, Power } from 'lucide-react';
import type { CSSProperties } from 'react';
import { Link } from 'react-router-dom';
import type { RoomSummary } from '../../api/types';
import { resolveRoomImage } from '../../config/roomImages';
import { formatPercent, formatTemperature, pluralize } from '../../lib/format';
import { QuickAction } from '../common/QuickAction';
import styles from './RoomHeader.module.css';

interface RoomHeaderProps {
  room: RoomSummary;
  onAllOff: () => void;
  allOffBusy: boolean;
}

/**
 * Hero della stanza: la stessa foto della card in Home (risolta da config/roomImages),
 * nome, temperatura prominente e stato sintetico. Le posizioni di crop cambiano per breakpoint.
 */
export function RoomHeader({ room, onAllOff, allOffBusy }: RoomHeaderProps) {
  const image = resolveRoomImage(room.name);
  const lit = room.lightsOn > 0;
  const imageStyle = {
    '--pos-desktop': image.position.desktop ?? 'center',
    '--pos-tablet': image.position.tablet ?? image.position.desktop ?? 'center',
    '--pos-mobile': image.position.mobile ?? image.position.tablet ?? 'center',
  } as CSSProperties;

  const facts: string[] = [];
  if (room.lightsTotal > 0) {
    facts.push(room.lightsOn === 0 ? 'Luci spente' : pluralize(room.lightsOn, 'luce accesa', 'luci accese'));
  }
  if (room.shutterPosition !== undefined) facts.push(`Tapparelle ${formatPercent(room.shutterPosition)}`);
  if (room.humidity !== undefined) facts.push(`Umidità ${formatPercent(room.humidity)}`);
  facts.push(pluralize(room.deviceCount, 'dispositivo', 'dispositivi'));

  return (
    <>
      <header className={`${styles.hero} ${lit ? styles.lit : ''}`}>
        <img className={styles.image} style={imageStyle} src={image.src} alt="" />

        <Link to="/" className={styles.back} aria-label="Torna alla casa">
          <ChevronLeft size={26} aria-hidden="true" />
        </Link>

        <div className={styles.copy}>
          <h1 className={styles.title}>{room.name}</h1>
          <p className={styles.facts}>{facts.join(' · ')}</p>
        </div>

        {room.temperature !== undefined && (
          <div className={styles.climate}>
            <span className={styles.climateValue}>{formatTemperature(room.temperature)}</span>
            <span className={styles.climateLabel}>Temperatura attuale</span>
          </div>
        )}
      </header>

      <div className={styles.actions} role="group" aria-label="Azioni rapide">
        <QuickAction
          label="Spegni tutto"
          icon={<Power size={20} />}
          tone="danger"
          onClick={onAllOff}
          busy={allOffBusy}
          disabled={room.activeCount === 0}
        />
      </div>
    </>
  );
}
