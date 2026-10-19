import { Clapperboard, DoorOpen, Moon, Sparkles, Sunrise } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { EmptyState, ErrorState, LoadingState } from '../components/common/PageState';
import { useExecuteScene } from '../hooks/useActions';
import { useHome } from '../hooks/useHome';
import { errorMessage } from '../lib/apiErrors';
import type { Scene } from '../api/types';
import { ThemeToggle } from '../components/layout/ThemeToggle';
import styles from './CollectionPage.module.css';

/** Icone delle scene: il backend invia un nome generico, qui lo traduciamo in un'icona. */
const SCENE_ICONS: Record<string, LucideIcon> = {
  sunrise: Sunrise,
  movie: Clapperboard,
  moon: Moon,
  door: DoorOpen,
};

function sceneIcon(scene: Scene): LucideIcon {
  return (scene.icon && SCENE_ICONS[scene.icon]) || Sparkles;
}

/** Elenco delle scene della casa. Eseguire una scena è un'azione non distruttiva: nessuna conferma. */
export function ScenesPage() {
  const { data: home, error, isPending, isError } = useHome();
  const scene = useExecuteScene();

  if (isPending) return <LoadingState label="Caricamento delle scene" />;
  if (isError && !home) {
    return <ErrorState title="Impossibile caricare le scene" message={errorMessage(error)} />;
  }

  const scenes = home.scenes;

  return (
    <>
      <header className={styles.header}>
        <h1 className={styles.title}>Scene</h1>
        <p className={styles.subtitle}>Una scena applica più comandi insieme, con un solo tocco.</p>
              <ThemeToggle className={styles.themeSwitch} />
      </header>
      {scenes.length === 0 ? (
        <EmptyState title="Nessuna scena" message="Crea le scene nell'app SmartThings: compariranno qui." />
      ) : (
        <ul className={styles.sceneList}>
          {scenes.map((s) => {
            const Icon = sceneIcon(s);
            const running = scene.isPending && scene.variables?.id === s.id;
            return (
              <li key={s.id}>
                <button
                  type="button"
                  className={styles.scene}
                  onClick={() => scene.mutate(s)}
                  disabled={scene.isPending}
                  aria-busy={running}
                >
                  <span className={styles.sceneIcon} aria-hidden="true">
                    <Icon size={26} strokeWidth={1.8} />
                  </span>
                  <span className={styles.sceneText}>
                    <span className={styles.sceneName}>{s.name}</span>
                    <span className={styles.sceneHint}>{running ? 'In esecuzione…' : 'Tocca per avviare'}</span>
                  </span>
                </button>
              </li>
            );
          })}
        </ul>
      )}
    </>
  );
}
