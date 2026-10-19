import type { ReactNode } from 'react';
import { BottomNavigation } from './BottomNavigation';
import { SideNavigation } from './SideNavigation';
import styles from './AppShell.module.css';

/**
 * Struttura dell'app: rail di navigazione su tablet e desktop, barra in basso su smartphone.
 * Le due navigazioni sono diverse per progetto, non il desktop ridotto.
 */
export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className={styles.shell}>
      <SideNavigation />
      <main className={styles.main}>{children}</main>
      <BottomNavigation />
    </div>
  );
}
