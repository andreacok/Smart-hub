import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError } from './client';
import { resetRoomImage, uploadRoomImage } from './roomImageApi';

afterEach(() => vi.unstubAllGlobals());

function respond(status: number, body?: unknown) {
  const fetchMock = vi.fn(async () => new Response(body === undefined ? null : JSON.stringify(body), {
    status,
    headers: { 'Content-Type': 'application/json' },
  }));
  vi.stubGlobal('fetch', fetchMock);
  return fetchMock;
}

describe('roomImageApi', () => {
  it('invia il file come multipart senza impostare Content-Type JSON', async () => {
    const fetchMock = respond(200, { roomId: 'salotto', customImage: true, imageUrl: '/x?v=1' });

    const result = await uploadRoomImage('salotto', new Blob(['jpg'], { type: 'image/jpeg' }), 'foto.jpg');

    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe('/api/rooms/salotto/image');
    expect(init.method).toBe('POST');
    expect(init.body).toBeInstanceOf(FormData);
    expect((init.body as FormData).get('file')).toBeInstanceOf(File);
    expect((init.headers as Record<string, string>)['Content-Type']).toBeUndefined();
    expect(result.customImage).toBe(true);
  });

  it('propaga il messaggio dell\'errore del backend (es. file troppo grande)', async () => {
    respond(413, { status: 413, code: 'FILE_TOO_LARGE', message: 'La foto supera il limite di 10 MB.' });

    const error = await uploadRoomImage('salotto', new Blob(['x']), 'foto.jpg').catch((e: unknown) => e);

    expect(error).toBeInstanceOf(ApiError);
    expect((error as ApiError).code).toBe('FILE_TOO_LARGE');
    expect((error as ApiError).message).toBe('La foto supera il limite di 10 MB.');
  });

  it('il ripristino chiama DELETE sulla stessa risorsa', async () => {
    const fetchMock = respond(200, { roomId: 'cucina', customImage: false });

    await resetRoomImage('cucina');

    const [url, init] = fetchMock.mock.calls[0] as unknown as [string, RequestInit];
    expect(url).toBe('/api/rooms/cucina/image');
    expect(init.method).toBe('DELETE');
  });
});
