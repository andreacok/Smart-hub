import type { RoutineType } from '../../api/types';
import { useExecuteRoutine, useRoutines } from '../../hooks/useRoutines';
import { errorMessage, isUnreachable } from '../../lib/apiErrors';
import { QuickAction } from '../common/QuickAction';
import { EmptyState, ErrorState, LoadingState } from '../common/PageState';
import { RoutineCard } from './RoutineCard';
import styles from './RoutinesPanel.module.css';

const SECTIONS: { type: RoutineType; title: string }[] = [
  { type: 'MANUAL_ROUTINE', title: 'Routine' },
  { type: 'AUTOMATION', title: 'Automazioni' },
];

/** Tab "Routine Gruppi": routine e automazioni lette da SmartThings, con stati di caricamento, vuoto ed errore. */
export function RoutinesPanel() {
  const { data: routines, error, isPending, refetch } = useRoutines();
  const execute = useExecuteRoutine();

  if (isPending) return <LoadingState label="Caricamento delle routine" />;

  // Nessun dato e nessun caricamento in corso: la richiesta è fallita.
  if (!routines) {
    const unreachable = isUnreachable(error);
    return (
      <ErrorState
        title={unreachable ? 'Smart Hub non risponde' : 'Impossibile caricare le routine'}
        message={errorMessage(error)}
        action={<QuickAction label="Riprova" onClick={() => void refetch()} />}
      />
    );
  }

  if (routines.length === 0) {
    return (
      <EmptyState
        title="Nessuna routine"
        message="Crea routine e automazioni nell'app SmartThings: compariranno qui automaticamente."
      />
    );
  }

  return (
    <div className={styles.panel}>
      {SECTIONS.map(({ type, title }) => {
        const items = routines.filter((routine) => routine.type === type);
        if (items.length === 0) return null;
        return (
          <section key={type} aria-labelledby={`routines-${type}`}>
            <h2 id={`routines-${type}`} className={styles.sectionTitle}>{title}</h2>
            <div className={styles.grid}>
              {items.map((routine) => (
                <RoutineCard
                  key={routine.id}
                  routine={routine}
                  onRun={(item) => execute.mutate(item)}
                  running={execute.isPending && execute.variables?.id === routine.id}
                />
              ))}
            </div>
          </section>
        );
      })}
    </div>
  );
}
