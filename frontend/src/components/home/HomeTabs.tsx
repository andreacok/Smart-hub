import type { KeyboardEvent } from 'react';
import styles from './HomeTabs.module.css';

export type HomeTab = 'home' | 'routines';

const TABS: { id: HomeTab; label: string }[] = [
  { id: 'home', label: 'Casa' },
  { id: 'routines', label: 'Routine Gruppi' },
];

interface HomeTabsProps {
  value: HomeTab;
  onChange: (tab: HomeTab) => void;
}

/**
 * Navigazione a tab della schermata principale. Stesso linguaggio del selettore del tema:
 * segmento a pillola con stato attivo evidente. Le frecce spostano il focus tra i tab.
 */
export function HomeTabs({ value, onChange }: HomeTabsProps) {
  function handleKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    if (event.key !== 'ArrowLeft' && event.key !== 'ArrowRight') return;
    event.preventDefault();
    const current = TABS.findIndex((tab) => tab.id === value);
    const step = event.key === 'ArrowRight' ? 1 : -1;
    const next = TABS[(current + step + TABS.length) % TABS.length]!;
    onChange(next.id);
    document.getElementById(`tab-${next.id}`)?.focus();
  }

  return (
    <div role="tablist" aria-label="Sezioni" className={styles.list}>
      {TABS.map((tab) => {
        const selected = tab.id === value;
        return (
          <button
            key={tab.id}
            id={`tab-${tab.id}`}
            type="button"
            role="tab"
            aria-selected={selected}
            aria-controls={`panel-${tab.id}`}
            tabIndex={selected ? 0 : -1}
            className={`${styles.tab} ${selected ? styles.active : ''}`}
            onClick={() => onChange(tab.id)}
            onKeyDown={handleKeyDown}
          >
            {tab.label}
          </button>
        );
      })}
    </div>
  );
}
