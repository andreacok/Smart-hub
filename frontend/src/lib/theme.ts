/** Preferenza di tema scelta dall'utente. "system" segue il sistema operativo. */
export type ThemePreference = 'system' | 'light' | 'dark';

export const THEME_STORAGE_KEY = 'smarthub.theme';
export const THEME_PREFERENCES: readonly ThemePreference[] = ['system', 'light', 'dark'];

const THEME_COLOR: Record<'light' | 'dark', string> = {
  light: '#eef0f2',
  dark: '#111315',
};

export function isThemePreference(value: unknown): value is ThemePreference {
  return typeof value === 'string' && (THEME_PREFERENCES as readonly string[]).includes(value);
}

/** Legge la preferenza salvata. Storage bloccato o assente (modalità privata) → "system". */
export function readThemePreference(): ThemePreference {
  try {
    const stored = window.localStorage.getItem(THEME_STORAGE_KEY);
    return isThemePreference(stored) ? stored : 'system';
  } catch {
    return 'system';
  }
}

export function writeThemePreference(preference: ThemePreference): void {
  try {
    window.localStorage.setItem(THEME_STORAGE_KEY, preference);
  } catch {
    // Il tema resta valido per la sessione corrente anche senza storage.
  }
}

/** Tema effettivamente mostrato: con "system" dipende dal sistema operativo. */
export function resolveTheme(preference: ThemePreference, systemPrefersDark: boolean): 'light' | 'dark' {
  if (preference === 'system') return systemPrefersDark ? 'dark' : 'light';
  return preference;
}

function systemPrefersDark(): boolean {
  return window.matchMedia('(prefers-color-scheme: dark)').matches;
}

/**
 * Applica la preferenza al documento. Con "system" rimuove data-theme e lascia decidere il CSS,
 * così un cambio del sistema operativo si riflette subito senza ricaricare la pagina.
 */
export function applyThemePreference(preference: ThemePreference): void {
  const root = document.documentElement;
  if (preference === 'system') {
    delete root.dataset.theme;
  } else {
    root.dataset.theme = preference;
  }

  const meta = document.querySelector('meta[name="theme-color"]');
  if (meta) {
    meta.setAttribute('content', THEME_COLOR[resolveTheme(preference, systemPrefersDark())]);
  }
}
