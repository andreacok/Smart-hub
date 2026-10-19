import { LightbulbOff } from 'lucide-react';
import { ApiError } from '../api/client';
import type { Home } from '../api/types';
import { QuickAction } from '../components/common/QuickAction';
import { EmptyState, ErrorState, LoadingState } from '../components/common/PageState';
import { ActiveDevices } from '../components/home/ActiveDevices';
import { AlertList } from '../components/home/AlertList';
import { RoomCard } from '../components/home/RoomCard';
import { SceneCard } from '../components/home/SceneCard';
import { TopBar } from '../components/layout/TopBar';
import { useHomeLightsOff, useExecuteScene } from '../hooks/useActions';
import { useHome } from '../hooks/useHome';
import { formatTemperature, greeting, pluralize } from '../lib/format';
import styles from './HomePage.module.css';

function summaryLine(home: Home): string {
  const s = home.summary;
  const parts: string[] = [];
  parts.push(s.lightsOn === 0 ? 'Luci tutte spente' : pluralize(s.lightsOn, 'luce accesa', 'luci accese'));
  if (s.averageTemperature !== undefined) parts.push(`${formatTemperature(s.averageTemperature)} in casa`);
  if (s.alertCount > 0) parts.push(pluralize(s.alertCount, 'avviso', 'avvisi'));
  return parts.join(', ');
}

export function HomePage() {
  const { data: home, error, isPending, isError, refetch } = useHome();
  const scene = useExecuteScene();
  const lightsOff = useHomeLightsOff();

  if (isPending) return <LoadingState />;

  if (isError && !home) {
    const unreachable = error instanceof ApiError && (error.status === 0 || error.status >= 500);
    return (
      <ErrorState
        title={unreachable ? 'Il server Smart Hub non risponde' : 'Impossibile caricare la casa'}
        message={
          unreachable
            ? 'Verifica che il backend sia avviato (porta 8080) e raggiungibile da questo dispositivo.'
            : error.message
        }
        action={<QuickAction label="Riprova" onClick={() => void refetch()} />}
      />
    );
  }

  const name = home.ownerName?.trim();
  const title = name ? `${greeting()}, ${name}` : greeting();

  return (
    <>
      <TopBar title={title} subtitle={summaryLine(home)} offline={isError} demo={home.mode === 'mock'} />

      <div className={styles.layout}>
        <section className={styles.rooms} aria-labelledby="rooms-title">
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

        <aside className={styles.side}>
          {home.scenes.length > 0 && (
            <section aria-labelledby="scenes-title">
              <h2 id="scenes-title" className={styles.sectionTitle}>Scene</h2>
              <div className={styles.scenes}>
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

          <section aria-labelledby="active-title">
            <div className={styles.sectionHead}>
              <h2 id="active-title" className={styles.sectionTitle}>Acceso ora</h2>
              {home.summary.lightsOn > 0 && (
                <QuickAction
                  label="Spegni le luci"
                  icon={<LightbulbOff size={18} />}
                  onClick={() => lightsOff.mutate()}
                  busy={lightsOff.isPending}
                />
              )}
            </div>
            <ActiveDevices devices={home.activeDevices} />
          </section>

          <section aria-labelledby="alerts-title">
            <h2 id="alerts-title" className={styles.sectionTitle}>Avvisi</h2>
            <AlertList alerts={home.alerts} />
          </section>
        </aside>
      </div>
    </>
  );
}
