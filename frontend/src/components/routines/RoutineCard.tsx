import { Play, Sparkles, Workflow } from 'lucide-react';
import type { Routine, RoutineType } from '../../api/types';
import styles from './RoutineCard.module.css';

const TYPE_LABELS: Record<RoutineType, string> = {
  MANUAL_ROUTINE: 'Routine',
  AUTOMATION: 'Automazione',
};

interface RoutineCardProps {
  routine: Routine;
  onRun: (routine: Routine) => void;
  running: boolean;
}

/**
 * Card di una routine o automazione. Il pulsante Esegui compare solo quando SmartThings
 * permette l'esecuzione a richiesta: nessun controllo che non funziona.
 */
export function RoutineCard({ routine, onRun, running }: RoutineCardProps) {
  const Icon = routine.type === 'AUTOMATION' ? Workflow : Sparkles;

  return (
    <article className={styles.card}>
      <span className={styles.icon} aria-hidden="true">
        <Icon size={22} strokeWidth={1.8} />
      </span>
      <div className={styles.body}>
        <h3 className={styles.name}>{routine.name}</h3>
        <p className={styles.type}>{TYPE_LABELS[routine.type]}</p>
      </div>
      {routine.manuallyExecutable && (
        <button
          type="button"
          className={styles.run}
          onClick={() => onRun(routine)}
          disabled={running}
          aria-busy={running}
          aria-label={`Esegui ${routine.name}`}
        >
          <Play size={16} strokeWidth={2} aria-hidden="true" />
          <span>{running ? 'Avvio…' : 'Esegui'}</span>
        </button>
      )}
    </article>
  );
}
