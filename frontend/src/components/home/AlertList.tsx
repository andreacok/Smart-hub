import { ChevronRight } from 'lucide-react';
import { Link } from 'react-router-dom';
import type { Alert, AlertSeverity } from '../../api/types';
import styles from './AlertList.module.css';

/** La gravità si legge dal testo, non solo dal colore. */
const SEVERITY_LABEL: Record<AlertSeverity, string> = {
  INFO: 'Info',
  WARNING: 'Attenzione',
  CRITICAL: 'Critico',
};

/** Elenco degli avvisi come righe toccabili; ogni avviso porta alla stanza del dispositivo. */
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
            <span className={styles.text}>
              <span className={styles.title}>{alert.title}</span>
              <span className={styles.severity}>{SEVERITY_LABEL[alert.severity]}</span>
            </span>
            <ChevronRight size={20} aria-hidden="true" className={styles.chevron} />
          </Link>
        </li>
      ))}
    </ul>
  );
}
