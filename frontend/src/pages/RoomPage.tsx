import { Link, useParams } from 'react-router-dom';
import { ApiError } from '../api/client';
import { EmptyState, ErrorState, LoadingState } from '../components/common/PageState';
import { QuickAction } from '../components/common/QuickAction';
import { DeviceSection } from '../components/room/DeviceSection';
import { RoomHeader } from '../components/room/RoomHeader';
import { useRoomAllOff } from '../hooks/useActions';
import { useRoom } from '../hooks/useRoom';
import { groupDevices } from '../lib/grouping';
import styles from './RoomPage.module.css';

export function RoomPage() {
  const { roomId = '' } = useParams();
  const { data, error, isPending, isError, refetch } = useRoom(roomId);
  const allOff = useRoomAllOff(roomId);

  if (isPending) return <LoadingState label="Caricamento della stanza" />;

  if (isError && !data) {
    const notFound = error instanceof ApiError && error.status === 404;
    return (
      <ErrorState
        title={notFound ? 'Stanza non trovata' : 'Impossibile caricare la stanza'}
        message={notFound ? 'Potrebbe essere stata rinominata o eliminata in SmartThings.' : error.message}
        action={
          notFound ? (
            <Link to="/" className={styles.link}>Torna alla casa</Link>
          ) : (
            <QuickAction label="Riprova" onClick={() => void refetch()} />
          )
        }
      />
    );
  }

  const groups = groupDevices(data.devices);

  return (
    <>
      <RoomHeader room={data.room} onAllOff={() => allOff.mutate()} allOffBusy={allOff.isPending} />
      {isError && <p className={styles.stale}>Connessione persa: lo stato mostrato potrebbe non essere aggiornato.</p>}
      {groups.length === 0 ? (
        <EmptyState
          title="Nessun dispositivo in questa stanza"
          message="Assegna i dispositivi a questa stanza dall'app SmartThings."
        />
      ) : (
        groups.map((g) => <DeviceSection key={g.key} title={g.title} devices={g.devices} />)
      )}
    </>
  );
}
