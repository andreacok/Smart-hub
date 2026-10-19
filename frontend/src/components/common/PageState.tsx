import type { ReactNode } from 'react';
import styles from './PageState.module.css';

export function LoadingState({ label = 'Caricamento della casa' }: { label?: string }) {
  return (
    <div className={styles.state} role="status" aria-live="polite">
      <span className={styles.pulse} aria-hidden="true" />
      <p className={styles.muted}>{label}</p>
    </div>
  );
}

interface ErrorStateProps {
  title: string;
  message: string;
  action?: ReactNode;
}

export function ErrorState({ title, message, action }: ErrorStateProps) {
  return (
    <div className={styles.state} role="alert">
      <h2 className={styles.title}>{title}</h2>
      <p className={styles.muted}>{message}</p>
      {action}
    </div>
  );
}

export function EmptyState({ title, message, action }: ErrorStateProps) {
  return (
    <div className={styles.state}>
      <h2 className={styles.title}>{title}</h2>
      <p className={styles.muted}>{message}</p>
      {action}
    </div>
  );
}
