import { describe, expect, it } from 'vitest';
import { deviceStatusText, formatTemperature, greeting, shutterLabel } from './format';
import { light } from '../test/fixtures';

describe('format', () => {
  it('formats temperatures with Italian decimals', () => {
    expect(formatTemperature(22.45)).toBe('22,5°');
    expect(formatTemperature(undefined)).toBe('–');
  });

  it('describes shutters', () => {
    expect(shutterLabel('PARTIALLY_OPEN', 40)).toBe('Aperta al 40%');
    expect(shutterLabel('CLOSING', 40)).toBe('In chiusura');
  });

  it('describes a dimmed light', () => {
    expect(deviceStatusText(light({ state: { power: 'ON', brightness: 30 } }))).toBe('Accesa, 30%');
    expect(deviceStatusText(light({ online: false }))).toBe('Non raggiungibile');
  });

  it('greets according to the time of day', () => {
    expect(greeting(new Date(2026, 0, 1, 8))).toBe('Buongiorno');
    expect(greeting(new Date(2026, 0, 1, 21))).toBe('Buonasera');
  });
});
