import { Monitor, Moon, Sun } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { useTheme } from '../../hooks/useTheme';
import type { ThemePreference } from '../../lib/theme';
import styles from './ThemeToggle.module.css';

const OPTIONS: { value: ThemePreference; label: string; icon: LucideIcon }[] = [
  { value: 'system', label: 'Sistema', icon: Monitor },
  { value: 'light', label: 'Chiaro', icon: Sun },
  { value: 'dark', label: 'Scuro', icon: Moon },
];

interface ThemeToggleProps {
  /** Posizionamento del selettore nel contenitore che lo ospita. */
  className?: string;
}

/**
 * Selettore del tema a tre voci. Etichetta testuale oltre all'icona: lo stato non dipende dal solo simbolo.
 * "Sistema" segue il sistema operativo; le altre due forzano il tema.
 */
export function ThemeToggle({ className = '' }: ThemeToggleProps) {
  const { preference, setPreference } = useTheme();

  return (
    <div className={`${styles.group} ${className}`} role="group" aria-label="Tema">
      {OPTIONS.map(({ value, label, icon: Icon }) => {
        const active = preference === value;
        return (
          <button
            key={value}
            type="button"
            className={`${styles.option} ${active ? styles.active : ''}`}
            aria-pressed={active}
            onClick={() => setPreference(value)}
          >
            <Icon size={18} strokeWidth={1.8} aria-hidden="true" />
            <span className={styles.label}>{label}</span>
          </button>
        );
      })}
    </div>
  );
}
