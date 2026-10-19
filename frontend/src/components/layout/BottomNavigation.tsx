import { Ellipsis, House, LayoutGrid, Sparkles, SlidersHorizontal } from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import { NavLink } from 'react-router-dom';
import styles from './BottomNavigation.module.css';

interface NavItem {
  to: string;
  label: string;
  icon: LucideIcon;
  end?: boolean;
}

/** Le cinque sezioni principali dello smartphone, nello stesso ordine dei reference. */
const ITEMS: NavItem[] = [
  { to: '/', label: 'Home', icon: House, end: true },
  { to: '/rooms', label: 'Stanze', icon: LayoutGrid },
  { to: '/scenes', label: 'Scene', icon: Sparkles },
  { to: '/devices', label: 'Dispositivi', icon: SlidersHorizontal },
  { to: '/more', label: 'Altro', icon: Ellipsis },
];

/** Navigazione in basso per smartphone: cinque voci touch-friendly, stato attivo evidente. */
export function BottomNavigation() {
  return (
    <nav className={styles.nav} aria-label="Navigazione principale">
      <ul className={styles.list}>
        {ITEMS.map(({ to, label, icon: Icon, end }) => (
          <li key={to}>
            <NavLink to={to} end={end} className={({ isActive }) => `${styles.link} ${isActive ? styles.active : ''}`}>
              <span className={styles.pill}>
                <Icon size={22} strokeWidth={1.8} aria-hidden="true" />
              </span>
              <span className={styles.label}>{label}</span>
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
}
