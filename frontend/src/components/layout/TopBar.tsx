import type { ReactNode } from 'react';
import { useNow } from '../../hooks/useNow';
import styles from './TopBar.module.css';

interface TopBarProps {
  title: ReactNode;
  subtitle?: ReactNode;
  /** Contenuto a sinistra del titolo (es. pulsante indietro). */
  leading?: ReactNode;
  /** Mostra un indicatore quando il backend non risponde. */
  offline?: boolean;
  demo?: boolean;
}

export function TopBar({ title, subtitle, leading, offline = false, demo = false }: TopBarProps) {
  const now = useNow();
  const time = now.toLocaleTimeString('it-IT', { hour: '2-digit', minute: '2-digit' });
  const date = now.toLocaleDateString('it-IT', { weekday: 'long', day: 'numeric', month: 'long' });

  return (
    <header className={styles.bar}>
      <div className={styles.heading}>
        {leading}
        <div>
          <h1 className={styles.title}>{title}</h1>
          {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
        </div>
      </div>
      <div className={styles.meta}>
        {offline && <span className={styles.offline}>Connessione persa, nuovo tentativo in corso</span>}
        {demo && !offline && (
          <span className={styles.demo} title="Dati simulati: SMARTTHINGS_MODE=mock">
            Casa demo
          </span>
        )}
        <div className={styles.clock}>
          <span className={styles.time}>{time}</span>
          <span className={styles.date}>{date}</span>
        </div>
      </div>
    </header>
  );
}
