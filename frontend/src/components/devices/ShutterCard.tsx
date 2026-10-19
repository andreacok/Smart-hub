import { ChevronDown, ChevronUp, Pause } from 'lucide-react';
import type { Device } from '../../api/types';
import { useCommittedValue } from '../../hooks/useCommittedValue';
import { useDeviceCommand } from '../../hooks/useDeviceCommand';
import { shutterLabel } from '../../lib/format';
import { Slider } from '../common/Slider';
import { DeviceCard } from './DeviceCard';
import styles from './controls.module.css';

/** Tapparella: finestrella che mostra quanto è abbassata, comandi apri/stop/chiudi e posizione. */
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
    <DeviceCard
      device={device}
      status={shutterLabel(s.openState, s.position)}
      aside={hasPosition ? <ShutterWindow position={position} /> : undefined}
    >
      <div className={styles.segmented} role="group" aria-label={`Comandi ${device.name}`}>
        <button type="button" onClick={() => mutate({ device, command: { command: 'OPEN' } })}>
          <ChevronUp size={20} aria-hidden="true" /> Apri
        </button>
        {caps.includes('STOP') && (
          <button type="button" onClick={() => mutate({ device, command: { command: 'STOP' } })} data-emphasis={moving}>
            <Pause size={18} aria-hidden="true" /> Stop
          </button>
        )}
        <button type="button" onClick={() => mutate({ device, command: { command: 'CLOSE' } })}>
          <ChevronDown size={20} aria-hidden="true" /> Chiudi
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

/** Rappresentazione della finestra: la parte scura scende man mano che la tapparella si chiude. */
function ShutterWindow({ position }: { position: number }) {
  const closed = 100 - Math.max(0, Math.min(100, position));
  return (
    <span className={styles.window} aria-hidden="true">
      <span className={styles.blind} style={{ height: `${closed}%` }} />
      <span className={styles.windowLabel}>{Math.round(position)}%</span>
    </span>
  );
}
