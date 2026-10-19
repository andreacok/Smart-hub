import type { Device } from '../../api/types';
import { useCommittedValue } from '../../hooks/useCommittedValue';
import { useDeviceCommand } from '../../hooks/useDeviceCommand';
import { Slider } from '../common/Slider';
import { DeviceCard } from './DeviceCard';
import styles from './controls.module.css';

const COLOR_PRESETS = [
  { name: 'Ambra', hue: 36, saturation: 90 },
  { name: 'Rosso', hue: 4, saturation: 85 },
  { name: 'Viola', hue: 280, saturation: 70 },
  { name: 'Blu', hue: 220, saturation: 85 },
  { name: 'Verde', hue: 140, saturation: 70 },
];

/** Luce: tocco sull'intestazione per accendere/spegnere, slider per luminosità e temperatura colore. */
export function LightCard({ device }: { device: Device }) {
  const { mutate } = useDeviceCommand();
  const s = device.state;
  const on = s.power === 'ON';
  const dimmable = device.capabilities.includes('DIMMER');
  const hasColorTemp = device.capabilities.includes('COLOR_TEMPERATURE');
  const hasColor = device.capabilities.includes('COLOR');

  const [brightness, setBrightness] = useCommittedValue(s.brightness ?? 0, (value) =>
    mutate({ device, command: { command: 'SET_BRIGHTNESS', value } }),
  );
  const [kelvin, setKelvin] = useCommittedValue(s.colorTemperature ?? 2700, (value) =>
    mutate({ device, command: { command: 'SET_COLOR_TEMPERATURE', value } }),
  );

  return (
    <DeviceCard
      device={device}
      tone={on && device.online ? 'lit' : 'default'}
      onToggle={() => mutate({ device, command: { command: on ? 'TURN_OFF' : 'TURN_ON' } })}
      toggleLabel={`${device.name}: ${on ? 'spegni' : 'accendi'}`}
      pressed={on}
      status={on ? 'Accesa' : undefined}
      aside={on && dimmable ? `${brightness}%` : undefined}
    >
      {on && dimmable && (
        <Slider
          label={`Luminosità ${device.name}`}
          value={brightness}
          min={1}
          onChange={setBrightness}
          fill="var(--on-lamp)"
          valueText={`${brightness}%`}
        />
      )}
      {on && hasColorTemp && (
        <div className={styles.row}>
          <span className={styles.caption}>Calda</span>
          <Slider
            label={`Temperatura colore ${device.name}`}
            value={kelvin}
            min={2200}
            max={6500}
            step={100}
            onChange={setKelvin}
            fill="var(--on-lamp)"
            valueText={`${kelvin} K`}
          />
          <span className={styles.caption}>Fredda</span>
        </div>
      )}
      {on && hasColor && (
        <div className={styles.swatches} role="group" aria-label={`Colore ${device.name}`}>
          {COLOR_PRESETS.map((c) => {
            const selected = s.color !== undefined && Math.abs(s.color.hue - c.hue) < 8;
            return (
              <button
                key={c.name}
                type="button"
                className={`${styles.swatch} ${selected ? styles.swatchSelected : ''}`}
                style={{ background: `hsl(${c.hue} ${c.saturation}% 58%)` }}
                aria-label={c.name}
                aria-pressed={selected}
                onClick={() => mutate({ device, command: { command: 'SET_COLOR', value: { hue: c.hue, saturation: c.saturation } } })}
              />
            );
          })}
        </div>
      )}
    </DeviceCard>
  );
}
