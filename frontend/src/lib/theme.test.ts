import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import {
  THEME_STORAGE_KEY,
  applyThemePreference,
  isThemePreference,
  readThemePreference,
  resolveTheme,
  writeThemePreference,
} from './theme';

function mockSystemDark(dark: boolean) {
  vi.stubGlobal(
    'matchMedia',
    vi.fn((query: string) => ({
      matches: query.includes('dark') ? dark : false,
      media: query,
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
    })),
  );
}

describe('theme preference', () => {
  beforeEach(() => {
    window.localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    document.head.innerHTML = '<meta name="theme-color" content="#000000" />';
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('riconosce solo i valori validi', () => {
    expect(isThemePreference('system')).toBe(true);
    expect(isThemePreference('light')).toBe(true);
    expect(isThemePreference('dark')).toBe(true);
    expect(isThemePreference('blue')).toBe(false);
    expect(isThemePreference(null)).toBe(false);
  });

  it('usa "system" se non c\'è nulla salvato o il valore non è valido', () => {
    expect(readThemePreference()).toBe('system');
    window.localStorage.setItem(THEME_STORAGE_KEY, 'neon');
    expect(readThemePreference()).toBe('system');
  });

  it('ricorda la scelta salvata', () => {
    writeThemePreference('dark');
    expect(window.localStorage.getItem(THEME_STORAGE_KEY)).toBe('dark');
    expect(readThemePreference()).toBe('dark');
  });

  it('risolve "system" in base al sistema operativo', () => {
    expect(resolveTheme('system', true)).toBe('dark');
    expect(resolveTheme('system', false)).toBe('light');
    expect(resolveTheme('light', true)).toBe('light');
    expect(resolveTheme('dark', false)).toBe('dark');
  });

  it('con "system" rimuove data-theme e lascia decidere il CSS', () => {
    mockSystemDark(true);
    applyThemePreference('dark');
    expect(document.documentElement.dataset.theme).toBe('dark');

    applyThemePreference('system');
    expect(document.documentElement.hasAttribute('data-theme')).toBe(false);
  });

  it('aggiorna theme-color con il tema effettivo', () => {
    mockSystemDark(false);
    applyThemePreference('system');
    expect(document.querySelector('meta[name="theme-color"]')?.getAttribute('content')).toBe('#eef0f2');

    applyThemePreference('dark');
    expect(document.querySelector('meta[name="theme-color"]')?.getAttribute('content')).toBe('#111315');
  });
});
