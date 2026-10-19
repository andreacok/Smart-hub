// Tipi delle API REST del backend Smart Hub (modello normalizzato, indipendente da SmartThings).
// I campi opzionali vengono omessi dal backend quando non applicabili.

export type DeviceType = 'LIGHT' | 'SWITCH' | 'SHUTTER' | 'THERMOSTAT' | 'SENSOR' | 'CAMERA' | 'OTHER';

export type Capability =
  | 'POWER'
  | 'DIMMER'
  | 'COLOR_TEMPERATURE'
  | 'COLOR'
  | 'OPEN_CLOSE'
  | 'POSITION'
  | 'STOP'
  | 'TEMPERATURE'
  | 'HUMIDITY'
  | 'THERMOSTAT'
  | 'MOTION'
  | 'CONTACT'
  | 'BATTERY'
  | 'POWER_METER'
  | 'VIDEO';

export type PowerState = 'ON' | 'OFF';
export type OpenState = 'OPEN' | 'CLOSED' | 'OPENING' | 'CLOSING' | 'PARTIALLY_OPEN' | 'UNKNOWN';
export type ContactState = 'OPEN' | 'CLOSED';
export type OperatingState = 'HEATING' | 'COOLING' | 'IDLE' | 'FAN' | 'UNKNOWN';

export interface ColorValue {
  /** 0..360 */
  hue: number;
  /** 0..100 */
  saturation: number;
}

export interface DeviceState {
  power?: PowerState;
  brightness?: number;
  colorTemperature?: number;
  color?: ColorValue;
  openState?: OpenState;
  position?: number;
  temperature?: number;
  temperatureUnit?: string;
  humidity?: number;
  targetTemperature?: number;
  thermostatMode?: string;
  supportedThermostatModes?: string[];
  operatingState?: OperatingState;
  motion?: boolean;
  contact?: ContactState;
  battery?: number;
  powerUsage?: number;
}

export interface Device {
  id: string;
  name: string;
  roomId: string;
  roomName?: string;
  type: DeviceType;
  online: boolean;
  state: DeviceState;
  capabilities: Capability[];
  updatedAt?: string;
}

export interface RoomSummary {
  id: string;
  name: string;
  /** URL della fotografia personalizzata (versionato). Assente: si usa la foto predefinita. */
  imageUrl?: string;
  customImage?: boolean;
  deviceCount: number;
  onlineCount: number;
  activeCount: number;
  lightsOn: number;
  lightsTotal: number;
  temperature?: number;
  humidity?: number;
  shutterPosition?: number;
  shutterCount: number;
  openContacts: number;
  motion: boolean;
  alertCount: number;
}

export type AlertSeverity = 'INFO' | 'WARNING' | 'CRITICAL';
export type AlertKind = 'OFFLINE' | 'LOW_BATTERY' | 'OPEN_CONTACT';

export interface Alert {
  id: string;
  severity: AlertSeverity;
  kind: AlertKind;
  title: string;
  deviceId?: string;
  roomId?: string;
}

export interface RoomDetail {
  room: RoomSummary;
  devices: Device[];
  alerts: Alert[];
}

export interface Scene {
  id: string;
  name: string;
  icon?: string;
}

export interface HomeSummary {
  devicesTotal: number;
  devicesOnline: number;
  activeDevices: number;
  lightsOn: number;
  lightsTotal: number;
  averageTemperature?: number;
  averageHumidity?: number;
  openShutters: number;
  openContacts: number;
  powerUsage?: number;
  alertCount: number;
}

export interface Home {
  location?: { id: string; name: string };
  ownerName?: string;
  mode: 'mock' | 'live';
  summary: HomeSummary;
  rooms: RoomSummary[];
  alerts: Alert[];
  scenes: Scene[];
  activeDevices: Device[];
  updatedAt: string;
}

export type CommandType =
  | 'TURN_ON'
  | 'TURN_OFF'
  | 'SET_BRIGHTNESS'
  | 'SET_COLOR_TEMPERATURE'
  | 'SET_COLOR'
  | 'OPEN'
  | 'CLOSE'
  | 'STOP'
  | 'SET_POSITION'
  | 'SET_TARGET_TEMPERATURE'
  | 'SET_THERMOSTAT_MODE';

export type DeviceCommand =
  | { command: 'TURN_ON' | 'TURN_OFF' | 'OPEN' | 'CLOSE' | 'STOP' }
  | { command: 'SET_BRIGHTNESS' | 'SET_COLOR_TEMPERATURE' | 'SET_POSITION' | 'SET_TARGET_TEMPERATURE'; value: number }
  | { command: 'SET_COLOR'; value: ColorValue }
  | { command: 'SET_THERMOSTAT_MODE'; value: string };

export interface ActionResult {
  succeeded: number;
  failed: number;
  errors: string[];
}

export interface SystemStatus {
  mode: 'mock' | 'live';
  auth: string;
  connected: boolean;
  locationName?: string;
  roomCount: number;
  deviceCount: number;
  lastStructureRefresh?: string;
  lastStatusRefresh?: string;
  lastError?: string;
  lastErrorAt?: string;
}

export interface ApiErrorBody {
  status: number;
  code: string;
  message: string;
  path?: string;
}

/** Risposta di upload/ripristino della fotografia di una stanza. */
export interface RoomImage {
  roomId: string;
  customImage: boolean;
  imageUrl?: string;
  updatedAt?: string;
}
