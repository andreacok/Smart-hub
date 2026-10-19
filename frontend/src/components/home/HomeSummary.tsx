import type { Home } from '../../api/types';
import { formatTemperature } from '../../lib/format';
import styles from './HomeSummary.module.css';

/** Quattro numeri essenziali sopra le stanze: stato della casa, luci, temperatura, avvisi. */
export function HomeSummary({ home }: { home: Home }) {
  const s = home.summary;
  const hasAlerts = s.alertCount > 0;

  return (
    <section className={styles.summary} aria-label="Riepilogo casa">
      <div className={`${styles.tile} ${hasAlerts ? styles.attention : styles.ok}`}>
        <span className={styles.label}>Casa</span>
        <span className={styles.value}>{hasAlerts ? 'Attenzione' : 'OK'}</span>
        <span className={styles.caption}>{`${s.devicesOnline} di ${s.devicesTotal} online`}</span>
      </div>
      <div className={styles.tile}>
        <span className={styles.label}>Luci</span>
        <span className={styles.value}>{s.lightsOn}</span>
        <span className={styles.caption}>{`accese su ${s.lightsTotal}`}</span>
      </div>
      <div className={styles.tile}>
        <span className={styles.label}>Temperatura</span>
        <span className={styles.value}>{formatTemperature(s.averageTemperature)}</span>
        <span className={styles.caption}>media</span>
      </div>
      <div className={`${styles.tile} ${hasAlerts ? styles.alert : ''}`}>
        <span className={styles.label}>Avvisi</span>
        <span className={styles.value}>{s.alertCount}</span>
        <span className={styles.caption}>{hasAlerts ? 'da controllare' : 'nessuno'}</span>
      </div>
    </section>
  );
}
