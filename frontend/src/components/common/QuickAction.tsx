import type { ReactNode } from 'react';
import styles from './QuickAction.module.css';

interface QuickActionProps {
  label: string;
  icon?: ReactNode;
  onClick: () => void;
  busy?: boolean;
  disabled?: boolean;
}

/** Pulsante d'azione grande e touch-friendly (es. "Spegni tutto"). */
export function QuickAction({ label, icon, onClick, busy = false, disabled = false }: QuickActionProps) {
  return (
    <button type="button" className={styles.action} onClick={onClick} disabled={disabled || busy} aria-busy={busy}>
      {icon && <span className={styles.icon} aria-hidden="true">{icon}</span>}
      <span>{label}</span>
    </button>
  );
}
