import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import type { RoomDetail } from '../api/types';
import { LightCard } from '../components/devices/LightCard';
import { ToastProvider } from '../components/feedback/ToastProvider';
import { queryKeys } from '../lib/queryKeys';
import { light, roomDetail } from './fixtures';

function setup(response: () => Promise<Response>) {
  const fetchMock = vi.fn(response);
  vi.stubGlobal('fetch', fetchMock);
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: 0 } } });
  const device = light();
  client.setQueryData(queryKeys.room('soggiorno'), roomDetail([device]));

  // La card legge il dispositivo dalla cache, come nella pagina stanza.
  function Connected() {
    const room = client.getQueryData<RoomDetail>(queryKeys.room('soggiorno'));
    return room ? <LightCard device={room.devices[0]!} /> : null;
  }
  const wrapper = ({ children }: { children: ReactNode }) => (
    <QueryClientProvider client={client}>
      <ToastProvider>{children}</ToastProvider>
    </QueryClientProvider>
  );
  const utils = render(<Connected />, { wrapper });
  client.getQueryCache().subscribe(() => utils.rerender(<Connected />));
  return { fetchMock, client };
}

afterEach(() => {
  vi.unstubAllGlobals();
});

describe('LightCard', () => {
  it('turns the light on immediately and sends the command', async () => {
    let resolve: (r: Response) => void = () => undefined;
    const { fetchMock } = setup(() => new Promise<Response>((r) => (resolve = r)));
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', { name: 'Lampadario: accendi' }));

    // aggiornamento ottimistico: la UI cambia prima della risposta del server
    expect(await screen.findByRole('button', { name: 'Lampadario: spegni' })).toHaveAttribute('aria-pressed', 'true');
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/devices/soggiorno-lampadario/commands',
      expect.objectContaining({ method: 'POST', body: JSON.stringify({ command: 'TURN_ON' }) }),
    );

    resolve(new Response(JSON.stringify(light({ state: { power: 'ON', brightness: 60 } })), { status: 200 }));
  });

  it('rolls back and shows an error when the command fails', async () => {
    setup(async () =>
      new Response(JSON.stringify({ status: 502, code: 'SMARTTHINGS_ERROR', message: 'SmartThings non raggiungibile' }), {
        status: 502,
      }),
    );
    const user = userEvent.setup();

    await user.click(screen.getByRole('button', { name: 'Lampadario: accendi' }));

    expect(
      await screen.findByText('Impossibile accendere "Lampadario". SmartThings ha restituito un errore.'),
    ).toBeInTheDocument();
    await waitFor(() =>
      expect(screen.getByRole('button', { name: 'Lampadario: accendi' })).toHaveAttribute('aria-pressed', 'false'),
    );
  });
});
