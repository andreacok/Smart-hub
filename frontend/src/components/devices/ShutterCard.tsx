import { ChevronDown, ChevronUp, Pause } from 'lucide-react';
import type { Device } from '../../api/types';
import { useCommittedValue } from '../../hooks/useCommittedValue';
import { useDeviceCommand } from '../../hooks/useDeviceCommand';
import { shutterLabel } from '../../lib/format';
import { Slider } from '../common/Slider';
import { DeviceCard } from './DeviceCard';
import styles from './controls.module.css';

/**
 * Tapparella: la finestra mostra quanto è abbassata la tapparella (la parte coperta scende con la posizione),
 * il valore in grande, i comandi apri/stop/chiudi e lo slider di precisione.
 */
export function ShutterCard({ device }: { device: Device }) {
  const { mutate } = useDeviceCommand();
  const s = device.state;
  const caps = device.capabilities;
  const hasPosition = caps.includes('POSITION');
  const [position, setPosition] = useCommittedValue(s.position ?? 0, (value) =>
    mutate({ device, command: { command: 'SET_POSITION', value } }),
  );
  const moving = s.openState === 'OPENING' || s.openState === 'CLOSING';

  return (
    <DeviceCard device={device} status={shutterLabel(s.openState, s.position)}>
      {hasPosition && (
        <div className={styles.shutterWindow} aria-hidden="true">
          <div className={styles.blind} style={{ height: `${100 - position}%` }} />
        </div>
      )}
      {hasPosition && (
        <p className={styles.bigValue} aria-hidden="true">
          {Math.round(position)}%
        </p>
      )}
      <div className={styles.shutterCommands} role="group" aria-label={`Comandi ${device.name}`}>
        <button type="button" onClick={() => mutate({ device, command: { command: 'OPEN' } })}>
          <span className={styles.commandIcon}><ChevronUp size={22} aria-hidden="true" /></span>
          <span>Apri</span>
        </button>
        {caps.includes('STOP') && (
          <button type="button" onClick={() => mutate({ device, command: { command: 'STOP' } })} data-emphasis={moving}>
            <span className={styles.commandIcon}><Pause size={20} aria-hidden="true" /></span>
            <span>Stop</span>
          </button>
        )}
        <button type="button" onClick={() => mutate({ device, command: { command: 'CLOSE' } })}>
          <span className={styles.commandIcon}><ChevronDown size={22} aria-hidden="true" /></span>
          <span>Chiudi</span>
        </button>
      </div>
      {hasPosition && (
        <Slider
          label={`Apertura ${device.name}`}
          value={position}
          onChange={setPosition}
          fill="var(--cool)"
          valueText={`Aperta al ${position}%`}
        />
      )}
    </DeviceCard>
  );
}
