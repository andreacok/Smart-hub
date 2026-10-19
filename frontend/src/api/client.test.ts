import { afterEach, describe, expect, it, vi } from 'vitest';
import { ApiError, request } from './client';

function stubFetch(impl: () => Promise<Response>) {
  vi.stubGlobal('fetch', vi.fn(impl));
}

afterEach(() => vi.unstubAllGlobals());

describe('request: normalizzazione degli errori', () => {
  it('body strutturato del backend diventa un ApiError completo', async () => {
    stubFetch(async () => new Response(JSON.stringify({
      status: 424,
      code: 'SMARTTHINGS_DEVICE_OFFLINE',
      source: 'SMARTTHINGS',
      message: 'Il dispositivo non è raggiungibile tramite SmartThings.',
      detail: 'Il dispositivo risulta offline',
      retryable: true,
      deviceId: 'frigo-1',
      correlationId: 'cid-123',
      upstreamRequestId: 'st-9',
    }), { status: 424, headers: { 'Content-Type': 'application/json' } }));

    const error = await request('/api/devices/frigo-1/commands').catch((e: unknown) => e);
    expect(error).toBeInstanceOf(ApiError);
    const api = error as ApiError;
    expect(api.status).toBe(424);
    expect(api.code).toBe('SMARTTHINGS_DEVICE_OFFLINE');
    expect(api.source).toBe('SMARTTHINGS');
    expect(api.retryable).toBe(true);
    expect(api.deviceId).toBe('frigo-1');
    expect(api.correlationId).toBe('cid-123');
    expect(api.upstreamRequestId).toBe('st-9');
  });

  it('risposta non JSON (proxy 502): nessun "Errore 502" nel messaggio', async () => {
    stubFetch(async () => new Response('<html>Bad Gateway</html>', { status: 502 }));
    const error = (await request('/api/home').catch((e: unknown) => e)) as ApiError;
    expect(error.code).toBe('SMART_HUB_UNAVAILABLE');
    expect(error.message).not.toMatch(/502|Errore/);
    expect(error.retryable).toBe(true);
  });

  it('errore di rete del browser: messaggio comprensibile, non "Failed to fetch"', async () => {
    stubFetch(async () => { throw new TypeError('Failed to fetch'); });
    const error = (await request('/api/home').catch((e: unknown) => e)) as ApiError;
    expect(error.code).toBe('NETWORK_UNREACHABLE');
    expect(error.message).not.toMatch(/Failed to fetch/);
    expect(error.source).toBe('NETWORK');
  });

  it('timeout del client: NETWORK_TIMEOUT', async () => {
    stubFetch(async () => { throw new DOMException('aborted', 'AbortError'); });
    const error = (await request('/api/home').catch((e: unknown) => e)) as ApiError;
    expect(error.code).toBe('NETWORK_TIMEOUT');
    expect(error.retryable).toBe(true);
  });

  it('richiesta rifiutata senza body (es. CORS 403): messaggio di richiesta non accettata, non "errore inatteso"', async () => {
    stubFetch(async () => new Response('Invalid CORS request', { status: 403 }));
    const error = (await request('/api/home').catch((e: unknown) => e)) as ApiError;
    expect(error.code).toBe('SMART_HUB_VALIDATION_ERROR');
    expect(error.message).toBe('La richiesta non è stata accettata da Smart Hub.');
  });

  it('errore interno del backend senza body: fallback generico', async () => {
    stubFetch(async () => new Response('', { status: 500 }));
    const error = (await request('/api/home').catch((e: unknown) => e)) as ApiError;
    expect(error.code).toBe('SMART_HUB_INTERNAL_ERROR');
    expect(error.message).toBe('Si è verificato un errore inatteso.');
  });
});
