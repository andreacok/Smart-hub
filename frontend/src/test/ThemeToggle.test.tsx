import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ThemeToggle } from '../components/layout/ThemeToggle';
import { initTheme } from '../hooks/useTheme';

describe('ThemeToggle', () => {
  beforeEach(() => {
    window.localStorage.clear();
    document.documentElement.removeAttribute('data-theme');
    vi.stubGlobal(
      'matchMedia',
      vi.fn(() => ({ matches: false, media: '', addEventListener: vi.fn(), removeEventListener: vi.fn() })),
    );
    initTheme();
  });

  it('parte da "Sistema" e forza il tema scelto', async () => {
    const user = userEvent.setup();
    render(<ThemeToggle />);

    const system = screen.getByRole('button', { name: 'Sistema' });
    expect(system).toHaveAttribute('aria-pressed', 'true');
    expect(document.documentElement.hasAttribute('data-theme')).toBe(false);

    await user.click(screen.getByRole('button', { name: 'Scuro' }));
    expect(document.documentElement.dataset.theme).toBe('dark');
    expect(screen.getByRole('button', { name: 'Scuro' })).toHaveAttribute('aria-pressed', 'true');
    expect(window.localStorage.getItem('smarthub.theme')).toBe('dark');

    await user.click(screen.getByRole('button', { name: 'Chiaro' }));
    expect(document.documentElement.dataset.theme).toBe('light');
  });
});
