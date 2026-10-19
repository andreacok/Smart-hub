import { EmptyState, ErrorState, LoadingState } from '../components/common/PageState';
import { RoomCard } from '../components/home/RoomCard';
import { useHome } from '../hooks/useHome';
import { errorMessage } from '../lib/apiErrors';
import { pluralize } from '../lib/format';
import { ThemeToggle } from '../components/layout/ThemeToggle';
import styles from './CollectionPage.module.css';

/** Elenco di tutte le stanze, con la stessa card fotografica della Home. */
export function RoomsPage() {
  const { data: home, error, isPending, isError, refetch } = useHome();

  if (isPending) return <LoadingState label="Caricamento delle stanze" />;
  if (isError && !home) {
    return <ErrorState title="Impossibile caricare le stanze" message={errorMessage(error)} />;
  }

  return (
    <>
      <header className={styles.header}>
        <h1 className={styles.title}>Stanze</h1>
        <p className={styles.subtitle}>{pluralize(home.rooms.length, 'stanza', 'stanze')} nella casa</p>
              <ThemeToggle className={styles.themeSwitch} />
      </header>
      {home.rooms.length === 0 ? (
        <EmptyState
          title="Nessuna stanza"
          message="Crea le stanze nell'app SmartThings e assegna i dispositivi: compariranno qui automaticamente."
        />
      ) : (
        <div className={styles.roomGrid}>
          {home.rooms.map((room) => (
            <RoomCard key={room.id} room={room} />
          ))}
        </div>
      )}
      {isError && (
        <p className={styles.subtitle}>
          Connessione persa, lo stato mostrato potrebbe non essere aggiornato.{' '}
          <button type="button" onClick={() => void refetch()}>
            Riprova
          </button>
        </p>
      )}
    </>
  );
}
