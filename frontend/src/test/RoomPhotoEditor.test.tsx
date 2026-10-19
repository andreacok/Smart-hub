import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { act, render, renderHook, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReactNode } from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from '../api/client';
import type { RoomSummary } from '../api/types';
import { ToastContext } from '../components/feedback/toastContext';
import { RoomPhotoEditor } from '../components/room/RoomPhotoEditor';
import { useRoomPhotoEditor } from '../hooks/useRoomPhotoEditor';
import type { RoomPhotoEditor as Editor } from '../hooks/useRoomPhotoEditor';

vi.mock('../api/roomImageApi', () => ({
  uploadRoomImage: vi.fn(),
  resetRoomImage: vi.fn(),
}));
vi.mock('../lib/roomImageFile', async (importOriginal) => ({
  ...(await importOriginal<typeof import('../lib/roomImageFile')>()),
  prepareRoomImage: vi.fn(async () => new Blob(['jpg'], { type: 'image/jpeg' })),
}));

import { uploadRoomImage } from '../api/roomImageApi';

const room: RoomSummary = {
  id: 'salotto',
  name: 'Salotto',
  deviceCount: 3,
  onlineCount: 3,
  activeCount: 0,
  lightsOn: 0,
  lightsTotal: 1,
  shutterCount: 0,
  openContacts: 0,
  motion: false,
  alertCount: 0,
};

/** Imposta matchMedia per simulare smartphone (true) o desktop (false). */
function setCompact(compact: boolean) {
  window.matchMedia = vi.fn().mockImplementation((query: string) => ({
    matches: compact && query.includes('max-width'),
    media: query,
    addEventListener: vi.fn(),
    removeEventListener: vi.fn(),
    addListener: vi.fn(),
    removeListener: vi.fn(),
    onchange: null,
    dispatchEvent: vi.fn(),
  })) as unknown as typeof window.matchMedia;
}

/** Editor finto per testare solo la presentazione. */
function fakeEditor(overrides: Partial<Editor> = {}): Editor {
  return {
    pending: null,
    preparing: false,
    uploading: false,
    resetting: false,
    busy: false,
    hasCustomImage: false,
    confirmReset: false,
    setConfirmReset: vi.fn(),
    choose: vi.fn(async () => undefined),
    discard: vi.fn(),
    confirm: vi.fn(),
    resetToDefault: vi.fn(),
    ...overrides,
  } as Editor;
}

beforeEach(() => {
  URL.createObjectURL = vi.fn(() => 'blob:anteprima');
  URL.revokeObjectURL = vi.fn();
  setCompact(true);
});

afterEach(() => vi.clearAllMocks());

describe('RoomPhotoEditor', () => {
  it('su smartphone offre "Scatta foto" e "Scegli dalla galleria"', () => {
    render(<RoomPhotoEditor open roomName="Salotto" editor={fakeEditor()} onClose={() => undefined} />);
    expect(screen.getByRole('button', { name: 'Scatta foto' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'Scegli dalla galleria' })).toBeInTheDocument();
  });

  it('su desktop offre "Carica immagine" e non la fotocamera', () => {
    setCompact(false);
    render(<RoomPhotoEditor open roomName="Salotto" editor={fakeEditor()} onClose={() => undefined} />);
    expect(screen.getByRole('button', { name: 'Carica immagine' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: 'Scatta foto' })).not.toBeInTheDocument();
  });

  it('la fotocamera usa capture="environment", la galleria no', () => {
    const { container } = render(
      <RoomPhotoEditor open roomName="Salotto" editor={fakeEditor()} onClose={() => undefined} />,
    );
    const inputs = container.ownerDocument.querySelectorAll('input[type="file"]');
    const captures = Array.from(inputs).map((input) => input.getAttribute('capture'));
    expect(captures).toContain('environment');
    expect(captures.filter((c) => c === null)).toHaveLength(1);
    inputs.forEach((input) => expect(input.getAttribute('accept')).toBe('image/*'));
  });

  it('durante l\'upload il pulsante è disabilitato e mostra il caricamento', () => {
    const editor = fakeEditor({
      pending: { blob: new Blob(['x']), previewUrl: 'blob:anteprima' },
      uploading: true,
      busy: true,
    });
    render(<RoomPhotoEditor open roomName="Salotto" editor={editor} onClose={() => undefined} />);
    const button = screen.getByRole('button', { name: /Caricamento/ });
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute('aria-busy', 'true');
    expect(screen.getByRole('button', { name: 'Annulla' })).toBeDisabled();
  });

  it('"Ripristina foto predefinita" è nascosto senza foto personalizzata', () => {
    render(<RoomPhotoEditor open roomName="Salotto" editor={fakeEditor()} onClose={() => undefined} />);
    expect(screen.queryByRole('button', { name: 'Ripristina foto predefinita' })).not.toBeInTheDocument();
  });

  it('con foto personalizzata il ripristino chiede conferma prima di procedere', async () => {
    const editor = fakeEditor({ hasCustomImage: true });
    render(<RoomPhotoEditor open roomName="Salotto" editor={editor} onClose={() => undefined} />);
    await userEvent.click(screen.getByRole('button', { name: 'Ripristina foto predefinita' }));
    expect(editor.setConfirmReset).toHaveBeenCalledWith(true);
  });

  it('dopo la conferma il ripristino esegue la reset', async () => {
    const editor = fakeEditor({ hasCustomImage: true, confirmReset: true });
    render(<RoomPhotoEditor open roomName="Salotto" editor={editor} onClose={() => undefined} />);
    await userEvent.click(screen.getByRole('button', { name: 'Ripristina' }));
    expect(editor.resetToDefault).toHaveBeenCalled();
  });

  it('non è renderizzato quando è chiuso', () => {
    render(<RoomPhotoEditor open={false} roomName="Salotto" editor={fakeEditor()} onClose={() => undefined} />);
    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });
});

describe('useRoomPhotoEditor', () => {
  function setup() {
    const client = new QueryClient({ defaultOptions: { mutations: { retry: 0 } } });
    const show = vi.fn();
    const wrapper = ({ children }: { children: ReactNode }) => (
      <QueryClientProvider client={client}>
        <ToastContext.Provider value={{ show }}>{children}</ToastContext.Provider>
      </QueryClientProvider>
    );
    const { result } = renderHook(() => useRoomPhotoEditor(room), { wrapper });
    return { result, show };
  }

  it('il fallimento dell\'upload conserva l\'anteprima e mostra un toast', async () => {
    vi.mocked(uploadRoomImage).mockRejectedValueOnce(
      new ApiError(413, 'FILE_TOO_LARGE', 'La foto supera il limite di 10 MB.'),
    );
    const { result, show } = setup();

    await act(() => result.current.choose(new File(['x'], 'foto.jpg', { type: 'image/jpeg' })));
    expect(result.current.pending).not.toBeNull();

    act(() => result.current.confirm());

    await waitFor(() => expect(show).toHaveBeenCalledWith('La foto supera il limite di 10 MB.', 'error'));
    expect(result.current.pending).not.toBeNull();
    expect(result.current.uploading).toBe(false);
  });

  it('un upload riuscito chiude l\'anteprima e segnala il successo', async () => {
    vi.mocked(uploadRoomImage).mockResolvedValueOnce({
      roomId: 'salotto',
      customImage: true,
      imageUrl: '/api/rooms/salotto/image/content?v=1',
      updatedAt: '2026-10-20T00:00:00Z',
    });
    const { result, show } = setup();

    await act(() => result.current.choose(new File(['x'], 'foto.jpg', { type: 'image/jpeg' })));
    act(() => result.current.confirm());

    await waitFor(() => expect(show).toHaveBeenCalledWith('Foto della stanza aggiornata', 'success'));
    expect(result.current.pending).toBeNull();
    expect(URL.revokeObjectURL).toHaveBeenCalledWith('blob:anteprima');
  });
});
