import type { Device, DeviceFamily, DeviceType } from '../api/types';

/**
 * Comportamento UI del dispositivo: decide quale card usare. Derivato dal tipo normalizzato,
 * così la classificazione fine (forno, frigorifero...) non obbliga a nuove card per ogni categoria.
 * GENERIC: nessuna card dedicata, si usa la card generica con l'icona di tipo/famiglia.
 */
export type DeviceKind = 'LIGHT' | 'SWITCH' | 'SHUTTER' | 'THERMOSTAT' | 'SENSOR' | 'CAMERA' | 'GENERIC';

export function deviceKind(device: Pick<Device, 'deviceType' | 'deviceFamily'>): DeviceKind {
  const t = device.deviceType;
  if (t === 'LIGHT') return 'LIGHT';
  if (t === 'SWITCH' || t === 'SMART_PLUG') return 'SWITCH';
  if (t === 'BLIND' || t === 'SHADE') return 'SHUTTER';
  if (t === 'THERMOSTAT') return 'THERMOSTAT';
  if (t === 'CAMERA') return 'CAMERA';
  if (device.deviceFamily === 'SENSOR') return 'SENSOR';
  return 'GENERIC';
}

/** Nomi italiani dei tipi più comuni in casa. Gli altri tipi usano il nome della famiglia. */
const TYPE_LABELS: Partial<Record<DeviceType, string>> = {
  LIGHT: 'Luce',
  SWITCH: 'Interruttore',
  SMART_PLUG: 'Presa',
  BLIND: 'Tapparella',
  SHADE: 'Tenda',
  THERMOSTAT: 'Termostato',
  AIR_CONDITIONER: 'Climatizzatore',
  CAMERA: 'Videocamera',
  DOOR_BELL: 'Campanello',
  REFRIGERATOR: 'Frigorifero',
  CUBE_REFRIGERATOR: 'Frigorifero',
  KIMCHI_REFRIGERATOR: 'Frigorifero',
  WINE_CELLAR: 'Cantinetta',
  OVEN: 'Forno',
  MICROWAVE: 'Microonde',
  COOKTOP: 'Piano cottura',
  RANGE: 'Fornelli',
  STOVE: 'Fornelli',
  KITCHEN_HOOD: 'Cappa',
  COFFEE_MAKER: 'Macchina del caffè',
  WASHER: 'Lavatrice',
  DRYER: 'Asciugatrice',
  DISHWASHER: 'Lavastoviglie',
  ROBOT_CLEANER: 'Robot aspirapolvere',
  STICK_VACUUM_CLEANER: 'Aspirapolvere',
  TELEVISION: 'Televisore',
  SMART_LOCK: 'Serratura',
  SMART_MONITOR: 'Monitor',
  SPEAKER: 'Altoparlante',
  TEMP_SENSOR: 'Sensore temperatura',
  MOTION_SENSOR: 'Sensore movimento',
  CONTACT_SENSOR: 'Sensore apertura',
};

const FAMILY_LABELS: Record<DeviceFamily, string> = {
  ACCESS: 'Accesso',
  AIR_QUALITY: 'Qualità dell’aria',
  AUDIO: 'Audio',
  CLEANING: 'Pulizia',
  CLIMATE: 'Clima',
  COOKING_APPLIANCE: 'Cucina',
  COVERING: 'Copertura',
  FITNESS: 'Fitness',
  GENERIC: 'Dispositivo',
  LAUNDRY: 'Bucato',
  LIGHTING: 'Illuminazione',
  MEDIA: 'Multimedia',
  MOBILE_TRACKING: 'Localizzazione',
  NETWORK: 'Rete',
  PERSONAL_HEALTH: 'Salute',
  PET: 'Animali',
  POWER: 'Energia',
  REFRIGERATION: 'Refrigerazione',
  REMOTE_CONTROL: 'Telecomando',
  SECURITY: 'Sicurezza',
  SENSOR: 'Sensore',
  WATER: 'Acqua',
};

/** Etichetta leggibile del tipo: sempre il tipo corretto, anche senza una card dedicata. */
export function deviceTypeLabel(device: Pick<Device, 'deviceType' | 'deviceFamily'>): string {
  return TYPE_LABELS[device.deviceType] ?? FAMILY_LABELS[device.deviceFamily] ?? 'Dispositivo';
}
