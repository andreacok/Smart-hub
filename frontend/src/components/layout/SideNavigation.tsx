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
                  <span className="visually-hidden">{room.lightsOn} luci accese</span>
                </span>
              )}
              {room.alertCount > 0 && room.lightsOn === 0 && (
                <span className={styles.alert}>
                  <span className="visually-hidden">{room.alertCount} avvisi</span>
                </span>
              )}
            </NavLink>
          </li>
        ))}
      </ul>
    </nav>
  );
}
