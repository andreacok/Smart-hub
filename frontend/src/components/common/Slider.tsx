import type { CSSProperties } from 'react';
import styles from './Slider.module.css';

interface SliderProps {
  label: string;
  value: number;
  min?: number;
  max?: number;
  step?: number;
  onChange: (value: number) => void;
  disabled?: boolean;
  /** Colore della parte riempita (variabile CSS o colore). */
  fill?: string;
  valueText?: string;
}

/** Slider orizzontale grande, pensato per il tocco. */
export function Slider({ label, value, min = 0, max = 100, step = 1, onChange, disabled, fill, valueText }: SliderProps) {
  const percent = ((value - min) / (max - min)) * 100;
  const style = { '--percent': `${percent}%`, ...(fill ? { '--fill': fill } : {}) } as CSSProperties;
  return (
    <input
      type="range"
      className={styles.slider}
      style={style}
      aria-label={label}
      aria-valuetext={valueText}
      min={min}
      max={max}
      step={step}
      value={value}
      disabled={disabled}
      onChange={(e) => onChange(Number(e.target.value))}
      onClick={(e) => e.stopPropagation()}
    />
  );
}
