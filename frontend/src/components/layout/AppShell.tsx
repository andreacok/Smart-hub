import type { ReactNode } from 'react';
import { SideNavigation } from './SideNavigation';
import styles from './AppShell.module.css';

/** Struttura dell'app: navigazione stanze (rail su tablet/desktop, barra in basso su smartphone) + contenuto. */
export function AppShell({ children }: { children: ReactNode }) {
  return (
    <div className={styles.shell}>
      <SideNavigation />
      <main className={styles.main}>{children}</main>
    </div>
  );
}
