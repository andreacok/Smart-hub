import type { ApiErrorBody, ErrorSource } from './types';

/** Base URL delle API. Vuoto = stessa origine (proxy Vite in sviluppo, Nginx in produzione). */
const BASE_URL = (import.meta.env.VITE_API_BASE_URL ?? '').replace(/\/$/, '');
const DEFAULT_TIMEOUT_MS = 15_000;

/** Campi opzionali di un errore: presenti solo quando il backend li fornisce. */
export interface ApiErrorExtras {
  source?: ErrorSource;
  detail?: string;
  retryable?: boolean;
  deviceId?: string;
  retryAfterSeconds?: number;
  correlationId?: string;
  upstreamRequestId?: string;
}

/**
 * Errore normalizzato del client. {@code code} è il codice applicativo stabile (es. SMARTTHINGS_DEVICE_OFFLINE)
 * e {@code message} è già il testo pensato per l'utente. Lo status HTTP resta disponibile ma non va interpretato
 * al posto del codice.
 */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly source: ErrorSource;
  readonly detail?: string;
  readonly retryable: boolean;
  readonly deviceId?: string;
  readonly retryAfterSeconds?: number;
  readonly correlationId?: string;
  readonly upstreamRequestId?: string;

  constructor(status: number, code: string, message: string, extras: ApiErrorExtras = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.source = extras.source ?? 'UNKNOWN';
    this.detail = extras.detail;
    this.retryable = extras.retryable ?? false;
    this.deviceId = extras.deviceId;
    this.retryAfterSeconds = extras.retryAfterSeconds;
    this.correlationId = extras.correlationId;
    this.upstreamRequestId = extras.upstreamRequestId;
  }
}

function isErrorBody(value: unknown): value is ApiErrorBody {
  return typeof value === 'object' && value !== null && 'message' in value && 'code' in value;
}

/**
 * Errore per risposte non strutturate (proxy, pagine di errore, backend spento).
 * Non si mostra mai "Errore 502": il messaggio descrive la situazione.
 */
function fallbackError(status: number): ApiError {
  if (status === 502 || status === 503 || status === 504) {
    return new ApiError(status, 'SMART_HUB_UNAVAILABLE',
      'Smart Hub non è raggiungibile in questo momento. Riprova tra poco.', { source: 'SMART_HUB', retryable: true });
  }
  if (status >= 400 && status < 500) {
    // Richiesta rifiutata prima dell'applicazione (es. CORS, metodo o contenuto non ammessi).
    return new ApiError(status, 'SMART_HUB_VALIDATION_ERROR', 'La richiesta non è stata accettata da Smart Hub.',
      { source: 'SMART_HUB', retryable: false });
  }
  return new ApiError(status, 'SMART_HUB_INTERNAL_ERROR', 'Si è verificato un errore inatteso.',
    { source: 'SMART_HUB', retryable: false });
}

export async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), DEFAULT_TIMEOUT_MS);
  if (init.signal) {
    init.signal.addEventListener('abort', () => controller.abort());
  }

  let response: Response;
  try {
    response = await fetch(`${BASE_URL}${path}`, {
      ...init,
      signal: controller.signal,
      headers: {
        Accept: 'application/json',
        ...(typeof init.body === 'string' ? { 'Content-Type': 'application/json' } : {}),
        ...init.headers,
      },
    });
  } catch (error) {
    // Il fetch rifiuta solo per rete o timeout: il messaggio del browser ("Failed to fetch") non va mai mostrato.
    const aborted = error instanceof DOMException && error.name === 'AbortError';
    throw aborted
      ? new ApiError(0, 'NETWORK_TIMEOUT', 'Smart Hub non risponde. Riprova tra poco.',
          { source: 'NETWORK', retryable: true })
      : new ApiError(0, 'NETWORK_UNREACHABLE', 'Impossibile raggiungere Smart Hub. Controlla la connessione.',
          { source: 'NETWORK', retryable: true });
  } finally {
    clearTimeout(timeout);
  }

  if (response.status === 204) {
    return undefined as T;
  }

  const text = await response.text();
  let data: unknown = undefined;
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = undefined;
    }
  }

  if (!response.ok) {
    if (isErrorBody(data)) {
      throw new ApiError(response.status, data.code, data.message, {
        source: data.source,
        detail: data.detail,
        retryable: data.retryable,
        deviceId: data.deviceId,
        retryAfterSeconds: data.retryAfterSeconds,
        correlationId: data.correlationId,
        upstreamRequestId: data.upstreamRequestId,
      });
    }
    throw fallbackError(response.status);
  }
  return data as T;
}

export function get<T>(path: string, signal?: AbortSignal): Promise<T> {
  return request<T>(path, signal ? { signal } : {});
}

export function post<T>(path: string, body?: unknown): Promise<T> {
  return request<T>(path, {
    method: 'POST',
    ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
  });
}
