import { LightbulbOff } from 'lucide-react';
import { useState } from 'react';
import { errorMessage, isUnreachable } from '../lib/apiErrors';
import { QuickAction } from '../components/common/QuickAction';
import { EmptyState, ErrorState, LoadingState } from '../components/common/PageState';
import { AlertList } from '../components/home/AlertList';
import { HomeSummary } from '../components/home/HomeSummary';
import { HomeTabs, type HomeTab } from '../components/home/HomeTabs';
import { RoomCard } from '../components/home/RoomCard';
import { SceneCard } from '../components/home/SceneCard';
import { RoutinesPanel } from '../components/routines/RoutinesPanel';
import { TopBar } from '../components/layout/TopBar';
import { useHomeLightsOff, useExecuteScene } from '../hooks/useActions';
import { useHome } from '../hooks/useHome';
import { greeting } from '../lib/format';
import styles from './HomePage.module.css';

export function HomePage() {
  const { data: home, error, isPending, isError, refetch } = useHome();
  const scene = useExecuteScene();
  const lightsOff = useHomeLightsOff();
  const [tab, setTab] = useState<HomeTab>('home');

  if (isPending) return <LoadingState />;

  if (isError && !home) {
    const unreachable = isUnreachable(error);
    return (
      <ErrorState
        title={unreachable ? 'Smart Hub non risponde' : 'Impossibile caricare la casa'}
        message={
          unreachable
            ? 'Verifica che il backend sia avviato e raggiungibile da questo dispositivo.'
            : errorMessage(error)
        }
        action={<QuickAction label="Riprova" onClick={() => void refetch()} />}
      />
    );
  }

  const name = home.ownerName?.trim();
  const eyebrow = name ? `${greeting()}, ${name}` : greeting();

  return (
    <>
      <TopBar
        eyebrow={eyebrow}
        title={home.location?.name ?? 'Casa'}
        temperature={home.summary.averageTemperature}
        offline={isError}
        demo={home.mode === 'mock'}
      />

      <HomeTabs value={tab} onChange={setTab} />

      {tab === 'home' ? (
        <div role="tabpanel" id="panel-home" aria-labelledby="tab-home">
          <HomeSummary home={home} />

          <section aria-labelledby="rooms-title">
            <h2 id="rooms-title" className={styles.sectionTitle}>Stanze</h2>
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
          </section>

          {(home.scenes.length > 0 || home.summary.lightsOn > 0) && (
            <section aria-labelledby="actions-title" className={styles.actions}>
              <h2 id="actions-title" className={styles.sectionTitle}>Azioni rapide</h2>
              <div className={styles.actionRow}>
                {home.summary.lightsOn > 0 && (
                  <QuickAction
                    label="Spegni le luci"
                    icon={<LightbulbOff size={20} />}
                    onClick={() => lightsOff.mutate()}
                    busy={lightsOff.isPending}
                  />
                )}
                {home.scenes.map((s) => (
                  <SceneCard
                    key={s.id}
                    scene={s}
                    onRun={(sc) => scene.mutate(sc)}
                    running={scene.isPending && scene.variables?.id === s.id}
                  />
                ))}
              </div>
            </section>
          )}

          {home.alerts.length > 0 && (
            <section aria-labelledby="alerts-title" className={styles.actions}>
              <h2 id="alerts-title" className={styles.sectionTitle}>Avvisi</h2>
              <AlertList alerts={home.alerts} />
            </section>
          )}
        </div>
      ) : (
        <div role="tabpanel" id="panel-routines" aria-labelledby="tab-routines">
          <RoutinesPanel />
        </div>
      )}
    </>
  );
}
