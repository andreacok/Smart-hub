import { useSyncExternalStore } from 'react';
import {
  applyThemePreference,
  readThemePreference,
  writeThemePreference,
  type ThemePreference,
} from '../lib/theme';

/*
 * Stato del tema condiviso da tutti i selettori: un cambio in un punto si vede subito in tutti gli altri.
 * Vive fuori da React perché il tema va applicato anche prima del primo render.
 */
let preference: ThemePreference = 'system';
const listeners = new Set<() => void>();

function subscribe(listener: () => void): () => void {
  listeners.add(listener);
  return () => listeners.delete(listener);
}

function getSnapshot(): ThemePreference {
  return preference;
}

/** Da chiamare una sola volta all'avvio: legge la preferenza salvata e segue i cambi del sistema. */
export function initTheme(): void {
  preference = readThemePreference();
  applyThemePreference(preference);

  window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
    applyThemePreference(preference);
  });
}

export function setThemePreference(next: ThemePreference): void {
  if (next === preference) return;
  preference = next;
  writeThemePreference(next);
  applyThemePreference(next);
  listeners.forEach((listener) => listener());
}

export function useTheme(): { preference: ThemePreference; setPreference: (next: ThemePreference) => void } {
  const current = useSyncExternalStore(subscribe, getSnapshot, getSnapshot);
  return { preference: current, setPreference: setThemePreference };
}
