import type { Device, DeviceCommand, DeviceState, OpenState } from '../api/types';

const clamp = (v: number, min: number, max: number) => Math.min(max, Math.max(min, v));

function openStateFor(position: number): OpenState {
  if (position <= 0) return 'CLOSED';
  if (position >= 100) return 'OPEN';
  return 'PARTIALLY_OPEN';
}

/**
 * Stato atteso dopo un comando: usato per l'aggiornamento ottimistico della UI.
 * Rispecchia la logica di previsione del backend (CommandTranslator.predict).
 */
export function predictState(state: DeviceState, cmd: DeviceCommand): DeviceState {
  switch (cmd.command) {
    case 'TURN_ON':
      return { ...state, power: 'ON' };
    case 'TURN_OFF':
      return { ...state, power: 'OFF' };
    case 'SET_BRIGHTNESS': {
      const level = clamp(Math.round(cmd.value), 0, 100);
      return {
        ...state,
        brightness: level,
        ...(state.power !== undefined ? { power: level > 0 ? 'ON' : 'OFF' } : {}),
      };
    }
    case 'SET_COLOR_TEMPERATURE':
      return { ...state, colorTemperature: Math.round(cmd.value) };
    case 'SET_COLOR':
      return { ...state, color: cmd.value };
    case 'OPEN':
      return { ...state, openState: 'OPENING' };
    case 'CLOSE':
      return { ...state, openState: 'CLOSING' };
    case 'STOP':
      return { ...state, openState: state.position === undefined ? 'UNKNOWN' : openStateFor(state.position) };
    case 'SET_POSITION': {
      const target = clamp(Math.round(cmd.value), 0, 100);
      const from = state.position;
      const openState: OpenState =
        from === undefined || from === target ? openStateFor(target) : target > from ? 'OPENING' : 'CLOSING';
      return { ...state, position: target, openState };
    }
    case 'SET_TARGET_TEMPERATURE':
      return { ...state, targetTemperature: Math.round(cmd.value * 2) / 2 };
    case 'SET_THERMOSTAT_MODE':
      return { ...state, thermostatMode: cmd.value.toLowerCase() };
  }
}

export function predictDevice(device: Device, cmd: DeviceCommand): Device {
  return { ...device, state: predictState(device.state, cmd) };
}

/** Un dispositivo è "attivo" se è una luce o una presa accesa (stessa regola del backend). */
export function isActive(device: Device): boolean {
  return (
    device.online &&
    device.capabilities.includes('POWER') &&
    device.state.power === 'ON' &&
    (device.type === 'LIGHT' || device.type === 'SWITCH')
  );
}
