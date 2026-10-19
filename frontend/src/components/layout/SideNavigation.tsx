import { Lightbulb, TriangleAlert } from 'lucide-react';
import { NavLink } from 'react-router-dom';
import { useHome } from '../../hooks/useHome';
import styles from './SideNavigation.module.css';

/**
 * Navigazione tra casa e stanze. Ogni stanza mostra un piccolo punto caldo se ha luci accese:
 * si capisce dove c'è luce senza aprire nulla.
 */
export function SideNavigation() {
  const { data } = useHome();
  const rooms = data?.rooms ?? [];

  return (
    <nav className={styles.nav} aria-label="Stanze">
      <div className={styles.brand}>
        <span className={styles.brandMark} aria-hidden="true" />
        <span>{data?.location?.name ?? 'Smart Hub'}</span>
      </div>
      <ul className={styles.list}>
        <li>
          <NavLink to="/" end className={({ isActive }) => `${styles.link} ${isActive ? styles.active : ''}`}>
            Casa
          </NavLink>
        </li>
        {rooms.map((room) => (
          <li key={room.id}>
            <NavLink
              to={`/rooms/${encodeURIComponent(room.id)}`}
              className={({ isActive }) => `${styles.link} ${isActive ? styles.active : ''}`}
            >
              <span className={styles.label}>{room.name}</span>
              {room.lightsOn > 0 && (
                <span className={styles.lit} title={`${room.lightsOn} luci accese`}>
                  <Lightbulb size={14} aria-hidden="true" />
                  {room.lightsOn}
                  <span className="visually-hidden"> luci accese</span>
                </span>
              )}
              {room.alertCount > 0 && (
                <span className={styles.alert} title={`${room.alertCount} avvisi`}>
                  <TriangleAlert size={14} aria-hidden="true" />
                  {room.alertCount}
                  <span className="visually-hidden"> avvisi</span>
                </span>
              )}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
}
