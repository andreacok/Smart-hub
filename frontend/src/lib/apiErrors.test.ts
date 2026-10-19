import { describe, expect, it } from 'vitest';
import { ApiError } from '../api/client';
import type { Device } from '../api/types';
import { actionErrorMessage, commandErrorMessage, errorMessage, isUnreachable } from './apiErrors';

const lamp: Device = {
  id: 'soggiorno-lampadario',
  name: 'Luce soggiorno',
  roomId: 'soggiorno',
  deviceType: 'LIGHT',
  deviceFamily: 'LIGHTING',
  online: true,
  state: {},
  capabilities: ['POWER'],
};

const apiError = (code: string, message = 'testo del backend', status = 424) => new ApiError(status, code, message);

describe('errorMessage', () => {
  it('device offline: messaggio umano, non lo status HTTP', () => {
    const text = errorMessage(apiError('SMARTTHINGS_DEVICE_OFFLINE'));
    expect(text).toBe('Il dispositivo non è raggiungibile tramite SmartThings.');
    expect(text).not.toMatch(/424|Errore/);
  });

  it('timeout di SmartThings', () => {
    expect(errorMessage(apiError('SMARTTHINGS_TIMEOUT', 'x', 504))).toBe('SmartThings non ha risposto in tempo. Riprova.');
  });

  it('autorizzazione SmartThings', () => {
    expect(errorMessage(apiError('SMARTTHINGS_UNAUTHORIZED', 'x', 502))).toMatch(/non è autorizzata/);
  });

  it('capability non supportata', () => {
    expect(errorMessage(apiError('SMARTTHINGS_CAPABILITY_NOT_SUPPORTED', 'x', 422)))
      .toBe('Questo dispositivo non supporta questa funzione.');
  });

  it('rate limit', () => {
    expect(errorMessage(apiError('SMARTTHINGS_RATE_LIMITED', 'x', 429))).toBe('Troppe richieste a SmartThings. Riprova tra poco.');
  });

  it('codice sconosciuto: usa il messaggio del backend, già pensato per l\'utente', () => {
    expect(errorMessage(apiError('FILE_TOO_LARGE', 'La foto supera il limite di 10 MB.', 413)))
      .toBe('La foto supera il limite di 10 MB.');
  });

  it('errore non strutturato o non ApiError: fallback comprensibile, mai "Failed to fetch"', () => {
    expect(errorMessage(new TypeError('Failed to fetch'))).toBe('Si è verificato un errore inatteso.');
    expect(errorMessage(undefined)).toBe('Si è verificato un errore inatteso.');
  });
});

describe('isUnreachable', () => {
  it('riconosce backend e rete non raggiungibili', () => {
    expect(isUnreachable(new ApiError(0, 'NETWORK_UNREACHABLE', 'x'))).toBe(true);
    expect(isUnreachable(apiError('SMART_HUB_UNAVAILABLE', 'x', 502))).toBe(true);
    expect(isUnreachable(apiError('SMARTTHINGS_DEVICE_OFFLINE'))).toBe(false);
  });
});

describe('messaggi con contesto dell\'azione', () => {
  it('comando fallito: indica azione, dispositivo e motivo', () => {
    expect(commandErrorMessage(lamp, 'TURN_ON', apiError('SMARTTHINGS_DEVICE_OFFLINE')))
      .toBe('Impossibile accendere "Luce soggiorno". Il dispositivo non è raggiungibile tramite SmartThings.');
  });

  it('azione su più dispositivi', () => {
    expect(actionErrorMessage('spegnere le luci della casa', apiError('SMARTTHINGS_UNAVAILABLE', 'x', 503)))
      .toBe('Impossibile spegnere le luci della casa. SmartThings è temporaneamente non disponibile.');
  });
});
