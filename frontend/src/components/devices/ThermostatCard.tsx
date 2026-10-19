import { Flame, Minus, Plus, Snowflake } from 'lucide-react';
import type { Device } from '../../api/types';
import { useCommittedValue } from '../../hooks/useCommittedValue';
import { useDeviceCommand } from '../../hooks/useDeviceCommand';
import { formatTemperature, thermostatModeLabel } from '../../lib/format';
import { StatusBadge } from '../common/StatusBadge';
import { DeviceCard } from './DeviceCard';
import styles from './controls.module.css';

const STEP = 0.5;
const MIN_TARGET = 5;
const MAX_TARGET = 30;

/** Termostato: temperatura attuale, obiettivo regolabile a passi di 0,5° e modalità. */
export function ThermostatCard({ device }: { device: Device }) {
  const { mutate } = useDeviceCommand();
  const s = device.state;
  const [target, setTarget] = useCommittedValue(
    s.targetTemperature ?? 20,
    (value) => mutate({ device, command: { command: 'SET_TARGET_TEMPERATURE', value } }),
    700,
  );
  const off = s.thermostatMode === 'off';
  const adjust = (delta: number) =>
    setTarget(Math.min(MAX_TARGET, Math.max(MIN_TARGET, Math.round((target + delta) / STEP) * STEP)));

  const opBadge =
    s.operatingState === 'HEATING' ? (
      <StatusBadge tone="heat"><Flame size={14} aria-hidden="true" /> Riscalda</StatusBadge>
    ) : s.operatingState === 'COOLING' ? (
      <StatusBadge tone="cool"><Snowflake size={14} aria-hidden="true" /> Raffresca</StatusBadge>
    ) : (
      <StatusBadge>In pausa</StatusBadge>
    );

  return (
    <DeviceCard
      device={device}
      wide
      status={device.online ? '' : undefined}
      aside={opBadge}
    >
      <p className={styles.bigValue} aria-label={`Temperatura attuale ${formatTemperature(s.temperature, s.temperatureUnit)}`}>
        {formatTemperature(s.temperature, s.temperatureUnit)}
      </p>
      <div className={styles.thermostat}>
        <button
          type="button"
          className={styles.round}
          onClick={() => adjust(-STEP)}
          disabled={off || target <= MIN_TARGET}
          aria-label="Abbassa temperatura"
        >
          <Minus size={24} aria-hidden="true" />
        </button>
        <div className={styles.target} aria-live="polite">
          <span className={styles.targetValue}>{off ? 'Spento' : formatTemperature(target)}</span>
          <span className={styles.caption}>Temperatura desiderata</span>
        </div>
        <button
          type="button"
          className={styles.round}
          onClick={() => adjust(STEP)}
          disabled={off || target >= MAX_TARGET}
          aria-label="Alza temperatura"
        >
          <Plus size={24} aria-hidden="true" />
        </button>
      </div>
      {s.supportedThermostatModes && s.supportedThermostatModes.length > 0 && (
        <div className={styles.chips} role="group" aria-label="Modalità">
          {s.supportedThermostatModes.map((mode) => (
            <button
              key={mode}
              type="button"
              className={styles.chip}
              aria-pressed={s.thermostatMode === mode}
              onClick={() => mutate({ device, command: { command: 'SET_THERMOSTAT_MODE', value: mode } })}
            >
              {thermostatModeLabel(mode)}
            </button>
          ))}
        </div>
      )}
    </DeviceCard>
  );
}
