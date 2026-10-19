import {
  Blinds,
  Bot,
  Cctv,
  CookingPot,
  DoorClosed,
  DoorOpen,
  Droplets,
  Dumbbell,
  Fan,
  Flame,
  Footprints,
  HeartPulse,
  Lightbulb,
  Lock,
  PawPrint,
  Plug,
  Power,
  Refrigerator,
  Router,
  Shirt,
  Smartphone,
  Snowflake,
  Sparkles,
  Speaker,
  Thermometer,
  Tv,
  UtensilsCrossed,
  Wind,
  WashingMachine,
  Zap,
  Microwave,
  ShieldCheck,
  Radio,
  AirVent,
} from 'lucide-react';
import type { LucideIcon } from 'lucide-react';
import type { Device, DeviceFamily, DeviceType } from '../../api/types';
import { deviceKind } from '../../lib/deviceKind';

/** Icona specifica per i tipi che la hanno: un forno non è mai un termostato. */
const TYPE_ICONS: Partial<Record<DeviceType, LucideIcon>> = {
  REFRIGERATOR: Refrigerator,
  CUBE_REFRIGERATOR: Refrigerator,
  KIMCHI_REFRIGERATOR: Refrigerator,
  WINE_CELLAR: Refrigerator,
  OVEN: Flame,
  RANGE: Flame,
  STOVE: Flame,
  COOKTOP: Flame,
  MICROWAVE: Microwave,
  KITCHEN_HOOD: Wind,
  RICE_COOKER: CookingPot,
  WASHER: WashingMachine,
  DRYER: Shirt,
  DISHWASHER: UtensilsCrossed,
  ROBOT_CLEANER: Bot,
  STICK_VACUUM_CLEANER: Sparkles,
  AIR_CONDITIONER: AirVent,
  FAN: Fan,
  TELEVISION: Tv,
  SMART_LOCK: Lock,
  SPEAKER: Speaker,
  SOUND_BAR: Speaker,
  WIFI_ROUTER: Router,
  REMOTE_CONTROLLER: Radio,
  PET_WATER_DISPENSER: Droplets,
  FEEDER: PawPrint,
  CAT_TOILET: PawPrint,
  MOBILE: Smartphone,
  SECURITY_PANEL: ShieldCheck,
  DOOR_BELL: DoorClosed,
  FITNESS_MAT: Dumbbell,
  TREADMILL: Dumbbell,
  HEALTH_TRACKER: HeartPulse,
  SMART_PLUG: Plug,
  BATHROOM_HEATER: Thermometer,
  HEAT_PUMP: Thermometer,
  ELECTRIC_VEHICLE_CHARGER: Zap,
};

/** Icona della famiglia, usata quando il tipo non ne ha una dedicata. */
const FAMILY_ICONS: Record<DeviceFamily, LucideIcon> = {
  ACCESS: DoorClosed,
  AIR_QUALITY: Wind,
  AUDIO: Speaker,
  CLEANING: Sparkles,
  CLIMATE: Thermometer,
  COOKING_APPLIANCE: CookingPot,
  COVERING: Blinds,
  FITNESS: Dumbbell,
  GENERIC: Power,
  LAUNDRY: Shirt,
  LIGHTING: Lightbulb,
  MEDIA: Tv,
  MOBILE_TRACKING: Smartphone,
  NETWORK: Router,
  PERSONAL_HEALTH: HeartPulse,
  PET: PawPrint,
  POWER: Zap,
  REFRIGERATION: Snowflake,
  REMOTE_CONTROL: Radio,
  SECURITY: Cctv,
  SENSOR: Thermometer,
  WATER: Droplets,
};

/** Icona del dispositivo: una sola per comportamento o tipo, utile al riconoscimento rapido sul tablet. */
export function DeviceIcon({ device, size = 22 }: { device: Device; size?: number }) {
  const props = { size, strokeWidth: 1.8, 'aria-hidden': true } as const;
  switch (deviceKind(device)) {
    case 'LIGHT':
      return <Lightbulb {...props} />;
    case 'SWITCH':
      return <Plug {...props} />;
    case 'SHUTTER':
      return <Blinds {...props} />;
    case 'THERMOSTAT':
      return <Thermometer {...props} />;
    case 'CAMERA':
      return <Cctv {...props} />;
    case 'SENSOR':
      if (device.capabilities.includes('CONTACT')) {
        return device.state.contact === 'OPEN' ? <DoorOpen {...props} /> : <DoorClosed {...props} />;
      }
      if (device.capabilities.includes('MOTION')) return <Footprints {...props} />;
      if (device.capabilities.includes('HUMIDITY') && !device.capabilities.includes('TEMPERATURE')) {
        return <Droplets {...props} />;
      }
      return <Thermometer {...props} />;
    default: {
      const Icon = TYPE_ICONS[device.deviceType] ?? FAMILY_ICONS[device.deviceFamily] ?? Power;
      return <Icon {...props} />;
    }
  }
}
