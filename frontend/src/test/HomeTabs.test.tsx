import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import type { Routine } from '../api/types';
import { ToastProvider } from '../components/feedback/ToastProvider';
import { HomePage } from '../pages/HomePage';
import { home } from './fixtures';

const ROUTINES: Routine[] = [
  { id: 'scene-buonanotte', name: 'Buonanotte', type: 'MANUAL_ROUTINE', manuallyExecutable: true },
  { id: 'scene-film', name: 'Film', type: 'MANUAL_ROUTINE', manuallyExecutable: true },
  { id: 'rule-luce', name: 'Luce corridoio al movimento', type: 'AUTOMATION', manuallyExecutable: true },
];

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } });
}

function smartThingsError(): Response {
  return json({
    status: 503,
    code: 'SMARTTHINGS_UNAVAILABLE',
    source: 'SMARTTHINGS',
    message: 'SmartThings è temporaneamente non disponibile.',
    retryable: true,
  }, 503);
}

interface BackendOptions {
  routines?: () => Response | Promise<Response>;
  execute?: () => Response | Promise<Response>;
}

/** Simula il backend per URL: la casa risponde sempre, le routine e l'esecuzione seguono le opzioni. */
function mockBackend({ routines = () => json(ROUTINES), execute = () => new Response(null, { status: 204 }) }: BackendOptions = {}) {
  const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
    const url = String(input);
    const method = init?.method ?? 'GET';
    if (url === '/api/home') return json(home());
    if (url === '/api/routines' && method === 'GET') return routines();
    if (url.startsWith('/api/routines/') && method === 'POST') return execute();
    return json({ status: 404, code: 'SMART_HUB_NOT_FOUND', message: 'Risorsa non trovata.' }, 404);
  });
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

function requestedUrls(fetchMock: ReturnType<typeof mockBackend>): string[] {
  return fetchMock.mock.calls.map(([input]) => String(input));
}

function renderHome() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: 0 } } });
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>
      <ToastProvider>
        <MemoryRouter>{children}</MemoryRouter>
      </ToastProvider>
    </QueryClientProvider>
  );
  return render(<HomePage />, { wrapper });
}

beforeEach(() => {
  window.matchMedia = vi.fn().mockImplementation((query: string) => ({
    matches: false,
    media: query,
    onchange: null,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    addListener: vi.fn(),
    removeListener: vi.fn(),
    dispatchEvent: vi.fn(),
  })) as unknown as typeof window.matchMedia;
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('HomePage tabs', () => {
  it('opens on the Casa tab with the dashboard and does not load routines yet', async () => {
    const fetchMock = mockBackend();
    renderHome();

    expect(await screen.findByRole('heading', { name: 'Stanze' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Casa' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tab', { name: 'Routine Gruppi' })).toHaveAttribute('aria-selected', 'false');
    expect(requestedUrls(fetchMock)).not.toContain('/api/routines');
  });

  it('switches to Routine Gruppi without reloading and fetches the routines on open', async () => {
    const fetchMock = mockBackend();
    const user = userEvent.setup();
    renderHome();
    await screen.findByRole('heading', { name: 'Stanze' });

    await user.click(screen.getByRole('tab', { name: 'Routine Gruppi' }));

    expect(await screen.findByRole('heading', { name: 'Routine' })).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: 'Automazioni' })).toBeInTheDocument();
    expect(screen.queryByRole('heading', { name: 'Stanze' })).not.toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Routine Gruppi' })).toHaveAttribute('aria-selected', 'true');
    expect(requestedUrls(fetchMock)).toContain('/api/routines');
  });

  it('shows routines with their type and an Esegui button only when executable', async () => {
    mockBackend({
      routines: () => json([...ROUTINES, { id: 'rule-manuale', name: 'Solo lettura', type: 'AUTOMATION', manuallyExecutable: false }]),
    });
    const user = userEvent.setup();
    renderHome();
    await screen.findByRole('heading', { name: 'Stanze' });
    await user.click(screen.getByRole('tab', { name: 'Routine Gruppi' }));

    const buonanotte = (await screen.findByRole('heading', { name: 'Buonanotte' })).closest('article')!;
    expect(within(buonanotte).getByText('Routine')).toBeInTheDocument();
    expect(within(buonanotte).getByRole('button', { name: 'Esegui Buonanotte' })).toBeInTheDocument();

    const automation = screen.getByRole('heading', { name: 'Luce corridoio al movimento' }).closest('article')!;
    expect(within(automation).getByText('Automazione')).toBeInTheDocument();

    const readOnly = screen.getByRole('heading', { name: 'Solo lettura' }).closest('article')!;
    expect(within(readOnly).queryByRole('button')).not.toBeInTheDocument();
  });

  it('shows a loading state while SmartThings is queried', async () => {
    mockBackend({ routines: () => new Promise<Response>(() => undefined) });
    const user = userEvent.setup();
    renderHome();
    await screen.findByRole('heading', { name: 'Stanze' });

    await user.click(screen.getByRole('tab', { name: 'Routine Gruppi' }));

    expect(await screen.findByText('Caricamento delle routine')).toBeInTheDocument();
  });

  it('shows a clean empty state when the location has no routines', async () => {
    mockBackend({ routines: () => json([]) });
    const user = userEvent.setup();
    renderHome();
    await screen.findByRole('heading', { name: 'Stanze' });

    await user.click(screen.getByRole('tab', { name: 'Routine Gruppi' }));

    expect(await screen.findByRole('heading', { name: 'Nessuna routine' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /Esegui/ })).not.toBeInTheDocument();
  });

  it('shows the SmartThings error with a retry action instead of an empty list', async () => {
    mockBackend({ routines: () => smartThingsError() });
    const user = userEvent.setup();
    renderHome();
    await screen.findByRole('heading', { name: 'Stanze' });

    await user.click(screen.getByRole('tab', { name: 'Routine Gruppi' }));

    const alert = await screen.findByRole('alert');
    expect(within(alert).getByRole('heading', { name: 'Impossibile caricare le routine' })).toBeInTheDocument();
    expect(within(alert).getByText('SmartThings è temporaneamente non disponibile.')).toBeInTheDocument();
    expect(within(alert).getByRole('button', { name: 'Riprova' })).toBeInTheDocument();
  });

  it('executes a routine through the backend and confirms it', async () => {
    const fetchMock = mockBackend();
    const user = userEvent.setup();
    renderHome();
    await screen.findByRole('heading', { name: 'Stanze' });
    await user.click(screen.getByRole('tab', { name: 'Routine Gruppi' }));

    await user.click(await screen.findByRole('button', { name: 'Esegui Buonanotte' }));

    expect(await screen.findByText('"Buonanotte" avviata')).toBeInTheDocument();
    const post = fetchMock.mock.calls.find(([, init]) => init?.method === 'POST');
    expect(String(post?.[0])).toBe('/api/routines/scene-buonanotte/execute');
  });

  it('reports a failed execution as a subtle error message', async () => {
    mockBackend({ execute: () => smartThingsError() });
    const user = userEvent.setup();
    renderHome();
    await screen.findByRole('heading', { name: 'Stanze' });
    await user.click(screen.getByRole('tab', { name: 'Routine Gruppi' }));

    await user.click(await screen.findByRole('button', { name: 'Esegui Film' }));

    expect(await screen.findByText(/Impossibile avviare "Film"/)).toBeInTheDocument();
  });

  it('moves between tabs with the arrow keys', async () => {
    mockBackend();
    const user = userEvent.setup();
    renderHome();
    await screen.findByRole('heading', { name: 'Stanze' });

    screen.getByRole('tab', { name: 'Casa' }).focus();
    await user.keyboard('{ArrowRight}');

    expect(await screen.findByRole('heading', { name: 'Routine' })).toBeInTheDocument();
    expect(screen.getByRole('tab', { name: 'Routine Gruppi' })).toHaveFocus();
    await waitFor(() => expect(screen.getByRole('tab', { name: 'Routine Gruppi' })).toHaveAttribute('aria-selected', 'true'));
  });
});
