import type { ReactNode } from 'react';
import { useNow } from '../../hooks/useNow';
import { formatTemperature } from '../../lib/format';
import { ThemeToggle } from './ThemeToggle';
import styles from './TopBar.module.css';

interface TopBarProps {
  /** Riga piccola sopra il titolo (es. il saluto). */
  eyebrow?: ReactNode;
  title: ReactNode;
  /** Contenuto a sinistra del titolo (es. pulsante indietro). */
  leading?: ReactNode;
  /** Temperatura mostrata accanto all'ora. */
  temperature?: number;
  /** Mostra un indicatore quando il backend non risponde. */
  offline?: boolean;
  demo?: boolean;
}

/** Barra superiore essenziale: a sinistra dove sei, a destra ora e temperatura. */
export function TopBar({ eyebrow, title, leading, temperature, offline = false, demo = false }: TopBarProps) {
  const now = useNow();
  const time = now.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' });

  return (
    <header className={styles.bar}>
      <div className={styles.heading}>
        {leading}
        <div className={styles.titleBlock}>
          {eyebrow && <p className={styles.eyebrow}>{eyebrow}</p>}
          <h1 className={styles.title}>{title}</h1>
        </div>
      </div>
      <div className={styles.meta}>
        {offline && <span className={styles.offline}>Connessione persa, nuovo tentativo in corso</span>}
        {demo && !offline && (
          <span className={styles.demo} title="Dati simulati: SMARTTHINGS_MODE=mock">
            Casa demo
          </span>
        )}
        <time className={styles.time} dateTime={now.toISOString()}>{time}</time>
        {temperature !== undefined && <span className={styles.temperature}>{formatTemperature(temperature)}</span>}
        <ThemeToggle />
      </div>
    </header>
  );
}
