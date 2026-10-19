import { EmptyState, ErrorState, LoadingState } from '../components/common/PageState';
import { DeviceSection } from '../components/room/DeviceSection';
import { useDevices } from '../hooks/useDevices';
import { groupDevices } from '../lib/grouping';
import { pluralize } from '../lib/format';
import styles from './CollectionPage.module.css';

/** Tutti i dispositivi della casa, raggruppati per tipo come nella Room View. */
export function DevicesPage() {
  const { data: devices, isPending, isError } = useDevices();

  if (isPending) return <LoadingState label="Caricamento dei dispositivi" />;
  if (isError && !devices) {
    return <ErrorState title="Impossibile caricare i dispositivi" message="Riprova tra qualche istante." />;
  }

  const groups = groupDevices(devices);
  const online = devices.filter((d) => d.online).length;

  return (
    <>
      <header className={styles.header}>
        <h1 className={styles.title}>Dispositivi</h1>
        <p className={styles.subtitle}>
          {pluralize(devices.length, 'dispositivo', 'dispositivi')} · {online} online
        </p>
      </header>
      {groups.length === 0 ? (
        <EmptyState title="Nessun dispositivo" message="Collega dispositivi nell'app SmartThings: compariranno qui." />
      ) : (
        groups.map((g) => <DeviceSection key={g.key} title={g.title} devices={g.devices} />)
      )}
    </>
  );
}
