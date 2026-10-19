import { ErrorState, LoadingState } from '../components/common/PageState';
import { useSystemStatus } from '../hooks/useSystemStatus';
import { relativeTime } from '../lib/format';
import { ThemeToggle } from '../components/layout/ThemeToggle';
import styles from './CollectionPage.module.css';

/** Altro: stato del sistema e della connessione con SmartThings. */
export function MorePage() {
  const { data: status, isPending, isError } = useSystemStatus();

  if (isPending) return <LoadingState label="Caricamento dello stato" />;
  if (isError && !status) {
    return <ErrorState title="Impossibile leggere lo stato" message="Il server Smart Hub non risponde." />;
  }

  const modeLabel = status.mode === 'mock' ? 'Demo' : 'SmartThings';
  const connection = status.connected ? 'Attiva' : 'Assente';

  return (
    <>
      <header className={styles.header}>
        <h1 className={styles.title}>Altro</h1>
        <p className={styles.subtitle}>Stato del sistema</p>
              <ThemeToggle className={styles.themeSwitch} />
      </header>

      <dl className={styles.stats}>
        <div className={styles.stat}>
          <dt className={styles.statLabel}>Stanze</dt>
          <dd className={styles.statValue}>{status.roomCount}</dd>
        </div>
        <div className={styles.stat}>
          <dt className={styles.statLabel}>Dispositivi</dt>
          <dd className={styles.statValue}>{status.deviceCount}</dd>
        </div>
        <div className={styles.stat}>
          <dt className={styles.statLabel}>Connessione</dt>
          <dd className={styles.statValue}>{connection}</dd>
        </div>
      </dl>

      <dl className={styles.infoList}>
        <div className={styles.infoRow}>
          <dt>Modalità</dt>
          <dd>{modeLabel}</dd>
        </div>
        <div className={styles.infoRow}>
          <dt>Casa</dt>
          <dd>{status.locationName ?? '–'}</dd>
        </div>
        <div className={styles.infoRow}>
          <dt>Aggiornamento stato</dt>
          <dd>{relativeTime(status.lastStatusRefresh) || '–'}</dd>
        </div>
        <div className={styles.infoRow}>
          <dt>Aggiornamento struttura</dt>
          <dd>{relativeTime(status.lastStructureRefresh) || '–'}</dd>
        </div>
        {status.lastError && (
          <div className={styles.infoRow}>
            <dt>Ultimo errore</dt>
            <dd>
              {status.lastError}
              {status.lastErrorAt ? ` · ${relativeTime(status.lastErrorAt)}` : ''}
            </dd>
          </div>
        )}
      </dl>
      {isError && <p className={styles.subtitle}>Stato non aggiornato: connessione persa.</p>}
    </>
  );
}
