import { describe, expect, it } from 'vitest';
import { isActive, predictState } from './predict';
import { light } from '../test/fixtures';

describe('predictState', () => {
  it('turns lights on and off', () => {
    expect(predictState({ power: 'OFF' }, { command: 'TURN_ON' }).power).toBe('ON');
    expect(predictState({ power: 'ON' }, { command: 'TURN_OFF' }).power).toBe('OFF');
  });

  it('brightness above zero switches the light on', () => {
    const next = predictState({ power: 'OFF', brightness: 0 }, { command: 'SET_BRIGHTNESS', value: 140 });
    expect(next).toMatchObject({ power: 'ON', brightness: 100 });
  });

  it('shutter position shows the direction of movement', () => {
    expect(predictState({ position: 20 }, { command: 'SET_POSITION', value: 80 }).openState).toBe('OPENING');
    expect(predictState({ position: 80 }, { command: 'SET_POSITION', value: 0 }).openState).toBe('CLOSING');
  });

  it('thermostat target is rounded to half degrees', () => {
    expect(predictState({}, { command: 'SET_TARGET_TEMPERATURE', value: 21.3 }).targetTemperature).toBe(21.5);
  });
});

describe('isActive', () => {
  it('only counts online lights and switches that are on', () => {
    expect(isActive(light({ state: { power: 'ON' } }))).toBe(true);
    expect(isActive(light({ state: { power: 'OFF' } }))).toBe(false);
    expect(isActive(light({ state: { power: 'ON' }, online: false }))).toBe(false);
  });
});
