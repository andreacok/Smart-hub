import type { ReactNode } from 'react';
import styles from './StatusBadge.module.css';

export type BadgeTone = 'neutral' | 'ok' | 'warn' | 'alert' | 'heat' | 'cool';

/** Piccola etichetta di stato (offline, batteria scarica, in riscaldamento...). */
export function StatusBadge({ tone = 'neutral', children }: { tone?: BadgeTone; children: ReactNode }) {
  return <span className={`${styles.badge} ${styles[tone]}`}>{children}</span>;
}
