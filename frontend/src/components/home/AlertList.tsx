import { Link } from 'react-router-dom';
import type { Alert } from '../../api/types';
import styles from './AlertList.module.css';

/** Elenco degli avvisi; ogni avviso porta alla stanza del dispositivo. */
export function AlertList({ alerts }: { alerts: Alert[] }) {
  if (alerts.length === 0) {
    return <p className={styles.empty}>Nessun avviso. Porte chiuse, batterie cariche, tutto raggiungibile.</p>;
  }
  return (
    <ul className={styles.list}>
      {alerts.map((alert) => (
        <li key={alert.id}>
          <Link to={alert.roomId ? `/rooms/${encodeURIComponent(alert.roomId)}` : '/'} className={styles.item}>
            <span className={`${styles.marker} ${styles[alert.severity]}`} aria-hidden="true" />
            <span>{alert.title}</span>
          </Link>
        </li>
      ))}
    </ul>
  );
}
