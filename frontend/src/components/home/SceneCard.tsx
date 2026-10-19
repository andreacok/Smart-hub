import { Clapperboard, LogOut, Moon, Sparkles, Sunrise } from 'lucide-react';
import type { Scene } from '../../api/types';
import styles from './SceneCard.module.css';

function SceneIcon({ icon }: { icon: string | undefined }) {
  const props = { size: 22, strokeWidth: 1.8, 'aria-hidden': true } as const;
  switch (icon) {
    case 'sunrise':
      return <Sunrise {...props} />;
    case 'movie':
      return <Clapperboard {...props} />;
    case 'moon':
      return <Moon {...props} />;
    case 'door':
      return <LogOut {...props} />;
    default:
      return <Sparkles {...props} />;
  }
}

interface SceneCardProps {
  scene: Scene;
  onRun: (scene: Scene) => void;
  running: boolean;
}

/** Pulsante di una scena SmartThings. */
export function SceneCard({ scene, onRun, running }: SceneCardProps) {
  return (
    <button type="button" className={styles.scene} onClick={() => onRun(scene)} disabled={running} aria-busy={running}>
      <span className={styles.icon}>
        <SceneIcon icon={scene.icon} />
      </span>
      <span>{scene.name}</span>
    </button>
  );
}
