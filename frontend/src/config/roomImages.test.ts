import { describe, expect, it } from 'vitest';
import { normalizeRoomName, resolveRoomImage, roomImageKeyFor } from './roomImages';

describe('normalizeRoomName', () => {
  it('rimuove spazi ai bordi, accenti e maiuscole', () => {
    expect(normalizeRoomName('  Città  ')).toBe('citta');
    expect(normalizeRoomName('Camera   da   Letto')).toBe('camera da letto');
  });
});

describe('roomImageKeyFor', () => {
  it.each([
    ['Salotto', 'salotto'],
    ['salotto', 'salotto'],
    [' Soggiorno ', 'salotto'],
    ['Camera da Letto', 'camera-da-letto'],
    ['CAMERA', 'camera-da-letto'],
    ['Cucina', 'cucina'],
    ['Bagno Grande', 'bagno-grande'],
    ['Bagno Piccolo', 'bagno-piccolo'],
    ['Corridoio', 'corridoio'],
    ['Cameretta', 'cameretta'],
    ['Living Room', 'salotto'],
    ['Kitchen', 'cucina'],
    ['Hallway', 'corridoio'],
    ['Master Bedroom', 'camera-da-letto'],
  ])('risolve "%s" in %s', (name, key) => {
    const resolved = roomImageKeyFor(name);
    expect(resolved).toEqual({ key, isFallback: false });
  });

  it.each([
    ['Ripostiglio'],
    [''],
    ['   '],
    [undefined],
    [null],
    ['Stanza sconosciuta da SmartThings'],
  ])('usa default-room per "%s"', (name) => {
    expect(roomImageKeyFor(name)).toEqual({ key: 'default-room', isFallback: true });
  });
});

describe('resolveRoomImage', () => {
  it('restituisce sempre una sorgente, anche per nomi sconosciuti', () => {
    const fallback = resolveRoomImage('Ripostiglio');
    expect(fallback.src).toBeTruthy();
    expect(fallback.src).toBe(resolveRoomImage(undefined).src);
    expect(fallback.src).toBe(resolveRoomImage('').src);
    expect(fallback.isFallback).toBe(true);
  });

  it('usa un asset diverso per ogni stanza ufficiale', () => {
    const sources = ['Salotto', 'Camera da Letto', 'Cucina', 'Bagno Grande', 'Bagno Piccolo', 'Corridoio', 'Cameretta'].map(
      (name) => resolveRoomImage(name).src,
    );
    expect(new Set(sources).size).toBe(sources.length);
  });

  it('espone le posizioni di crop configurate', () => {
    expect(resolveRoomImage('Salotto').position.mobile).toBe('35% 50%');
    expect(resolveRoomImage('Ripostiglio').position).toEqual({});
  });
});
