import { ChevronLeft, Pencil, Power } from 'lucide-react';
import type { CSSProperties } from 'react';
import { useState } from 'react';
import { Link } from 'react-router-dom';
import type { RoomSummary } from '../../api/types';
import { resolveRoomPhoto } from '../../config/roomImages';
import { useRoomPhotoEditor } from '../../hooks/useRoomPhotoEditor';
import { formatPercent, formatTemperature, pluralize } from '../../lib/format';
import { QuickAction } from '../common/QuickAction';
import { RoomPhotoEditor } from './RoomPhotoEditor';
import styles from './RoomHeader.module.css';

interface RoomHeaderProps {
  room: RoomSummary;
  onAllOff: () => void;
  allOffBusy: boolean;
}

/**
 * Hero della stanza: fotografia (personalizzata o predefinita, stessa risoluzione della Home),
 * nome, temperatura prominente e stato sintetico. Il pulsante matita apre il cambio foto.
 */
export function RoomHeader({ room, onAllOff, allOffBusy }: RoomHeaderProps) {
  const photo = resolveRoomPhoto(room);
  const [editing, setEditing] = useState(false);
  const editor = useRoomPhotoEditor(room, { onDone: () => setEditing(false) });
  const lit = room.lightsOn > 0;
  const imageStyle = {
    '--pos-desktop': photo.position.desktop ?? 'center',
    '--pos-tablet': photo.position.tablet ?? photo.position.desktop ?? 'center',
    '--pos-mobile': photo.position.mobile ?? photo.position.tablet ?? 'center',
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
        {/* key: la foto nuova sostituisce la precedente senza riusare l'elemento (e la cache del browser). */}
        <img key={photo.src} className={styles.image} style={imageStyle} src={photo.src} alt="" />

        <Link to="/" className={styles.back} aria-label="Torna alla casa">
          <ChevronLeft size={26} aria-hidden="true" />
        </Link>

        <button
          type="button"
          className={styles.edit}
          onClick={() => setEditing(true)}
          aria-label="Cambia fotografia della stanza"
          aria-haspopup="dialog"
        >
          <Pencil size={18} aria-hidden="true" />
        </button>

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

      <RoomPhotoEditor
        open={editing}
        roomName={room.name}
        editor={editor}
        onClose={() => setEditing(false)}
      />

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
