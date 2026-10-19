// Tipi delle API REST del backend Smart Hub (modello normalizzato, indipendente da SmartThings).
// I campi opzionali vengono omessi dal backend quando non applicabili.

/** Tipo normalizzato: una costante per ogni categoria SmartThings (vedi DeviceType nel backend). */
export type DeviceType =
  | 'AIR_CONDITIONER' |
  'AIR_PURIFIER' |
  'AIR_QUALITY_DETECTOR' |
  'AIR_PURIFY_HUMIDIFIER' |
  'BATHROOM_HEATER' |
  'BATTERY' |
  'BED' |
  'BIDET' |
  'BLIND' |
  'BLOOD_GLUCOSE_MONITOR' |
  'BLOOD_PRESSURE_MONITOR' |
  'BLUETOOTH_TRACKER' |
  'BRIDGES' |
  'BUTTON' |
  'CAMERA' |
  'CAT_TOILET' |
  'CHARGER' |
  'CHLORINE_SENSOR' |
  'CLOTHING_CARE_MACHINE' |
  'COFFEE_MAKER' |
  'CONTACT_SENSOR' |
  'COOKTOP' |
  'CUBE_REFRIGERATOR' |
  'CURB_POWER_METER' |
  'CYCLING_SENSOR' |
  'DEHUMIDIFIER' |
  'DELIVERY_ROBOT' |
  'DISHWASHER' |
  'DOOR' |
  'DOOR_BELL' |
  'DRYER' |
  'EARBUDS' |
  'ELECTRIC_VEHICLE_CHARGER' |
  'ELEVATOR' |
  'ELLIPTICAL' |
  'ENVIRONMENT_SENSOR' |
  'FAN' |
  'FAUCET' |
  'FEEDER' |
  'FITNESS_MAT' |
  'FLASHLIGHT' |
  'FLOW_SENSOR' |
  'GARAGE_DOOR' |
  'GAS_METER' |
  'GAS_VALVE' |
  'GENERIC_SENSOR' |
  'HEALTH_TRACKER' |
  'HEAT_PUMP' |
  'HEATED_MATTRESS_PAD' |
  'HEATING_COOLING_MAT' |
  'HOME_THEATER' |
  'HUB' |
  'HUMIDIFIER' |
  'HUMIDITY_SENSOR' |
  'INDOOR_CYCLE' |
  'IR_REMOTE' |
  'IRRIGATION' |
  'KIMCHI_REFRIGERATOR' |
  'KITCHEN_HOOD' |
  'LEAK_SENSOR' |
  'LIGHT' |
  'LIGHT_SENSOR' |
  'MEDICAL_THERMOMETER' |
  'MICRO_FIBER_FILTER' |
  'MICROWAVE' |
  'MOBILE' |
  'MOBILE_PRESENCE' |
  'MOTION_SENSOR' |
  'MULTI_FUNCTIONAL_SENSOR' |
  'MUSIC_SYSTEM' |
  'NETWORK_AUDIO' |
  'NETWORKING' |
  'OTHER' |
  'OVEN' |
  'PANIC_BUTTON' |
  'PET_WATER_DISPENSER' |
  'PH_SENSOR' |
  'PILLOW' |
  'PLANT_GROWER' |
  'POWER_MEASUREMENT_SENSOR' |
  'PRESENCE_SENSOR' |
  'PRINTER' |
  'PRINTER_MULTI_FUNCTION' |
  'PROJECTOR' |
  'PUMP' |
  'RAIN_SENSOR' |
  'RANGE' |
  'RECEIVER' |
  'REMOTE_CONTROLLER' |
  'REFRIGERATOR' |
  'RICE_COOKER' |
  'ROBOT_CLEANER' |
  'ROWER' |
  'SAFE' |
  'SCALE_TO_MEASURE_MASS_OF_HUMAN_BODY' |
  'SCANNER' |
  'SECURITY_PANEL' |
  'SET_TOP' |
  'SHADE' |
  'SHOES' |
  'SHOES_CARE_MACHINE' |
  'SHOWER' |
  'SIREN' |
  'SMART_LOCK' |
  'SMART_MONITOR' |
  'SMART_PLUG' |
  'SMOKE_DETECTOR' |
  'SOLAR_PANEL' |
  'SOUND_BAR' |
  'SOUND_MACHINE' |
  'SOUND_SENSOR' |
  'SPEAKER' |
  'STAIR_CLIMBER' |
  'STEP_MACHINE' |
  'STICK_VACUUM_CLEANER' |
  'STORAGE' |
  'STOVE' |
  'SWITCH' |
  'TAG_READER' |
  'TELEVISION' |
  'TEMP_HUMIDITY_SENSOR' |
  'TEMP_SENSOR' |
  'THERMOSTAT' |
  'TOILET' |
  'TOWEL_RACK' |
  'TRACKER' |
  'TREADMILL' |
  'UPNP_MEDIA_RENDERER' |
  'VENT' |
  'VISION_SENSOR' |
  'VOICE_ASSISTANCE' |
  'WASHER' |
  'WATER_FREEZE_DETECTOR' |
  'WATER_HEATER' |
  'WATER_PURIFIER' |
  'WATER_VALVE' |
  'WEATHER_STATION' |
  'WIFI_ROUTER' |
  'WINDOW' |
  'WINDOW_OPENER' |
  'WINE_CELLAR' |
  'ZONE_SENSOR' |
  'UNKNOWN';

export type DeviceFamily = 'ACCESS' | 'AIR_QUALITY' | 'AUDIO' | 'CLEANING' | 'CLIMATE' | 'COOKING_APPLIANCE' | 'COVERING' | 'FITNESS' | 'GENERIC' | 'LAUNDRY' | 'LIGHTING' | 'MEDIA' | 'MOBILE_TRACKING' | 'NETWORK' | 'PERSONAL_HEALTH' | 'PET' | 'POWER' | 'REFRIGERATION' | 'REMOTE_CONTROL' | 'SECURITY' | 'SENSOR' | 'WATER';

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
  /** Categoria originale SmartThings, preservata così com'è. Assente se SmartThings non la fornisce. */
  smartThingsCategory?: string | null;
  deviceType: DeviceType;
  deviceFamily: DeviceFamily;
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
