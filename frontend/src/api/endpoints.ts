import { get, post } from './client';
import type {
  ActionResult,
  Device,
  DeviceCommand,
  Home,
  RoomDetail,
  RoomSummary,
  Scene,
  SystemStatus,
} from './types';

const enc = encodeURIComponent;

/** Client tipizzato per le API REST del backend. */
export const api = {
  home: (signal?: AbortSignal) => get<Home>('/api/home', signal),
  rooms: (signal?: AbortSignal) => get<RoomSummary[]>('/api/rooms', signal),
  room: (roomId: string, signal?: AbortSignal) => get<RoomDetail>(`/api/rooms/${enc(roomId)}`, signal),
  devices: (signal?: AbortSignal) => get<Device[]>('/api/devices', signal),
  device: (deviceId: string, signal?: AbortSignal) => get<Device>(`/api/devices/${enc(deviceId)}`, signal),
  scenes: (signal?: AbortSignal) => get<Scene[]>('/api/scenes', signal),
  systemStatus: (signal?: AbortSignal) => get<SystemStatus>('/api/system/status', signal),

  sendCommand: (deviceId: string, command: DeviceCommand) =>
    post<Device>(`/api/devices/${enc(deviceId)}/commands`, command),
  executeScene: (sceneId: string) => post<void>(`/api/scenes/${enc(sceneId)}/execute`),
  roomAllOff: (roomId: string) => post<ActionResult>(`/api/rooms/${enc(roomId)}/actions/all-off`),
  homeLightsOff: () => post<ActionResult>('/api/home/actions/lights-off'),
};
