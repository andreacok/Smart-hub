import styles from './LightMeter.module.css';

/** Un punto per ogni luce della stanza, acceso se la luce è accesa. */
export function LightMeter({ on, total }: { on: number; total: number }) {
  if (total === 0) return null;
  const dots = Array.from({ length: total }, (_, i) => i < on);
  return (
    <span className={styles.meter} role="img" aria-label={`${on} luci accese su ${total}`}>
      {dots.map((lit, i) => (
        <span key={i} className={`${styles.dot} ${lit ? styles.lit : ''}`} />
      ))}
    </span>
  );
}
