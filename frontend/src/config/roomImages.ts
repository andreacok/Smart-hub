import bagnoGrande from '../assets/rooms/bagno-grande.webp';
import bagnoPiccolo from '../assets/rooms/bagno-piccolo.webp';
import cameraDaLetto from '../assets/rooms/camera-da-letto.webp';
import cameretta from '../assets/rooms/cameretta.webp';
import corridoio from '../assets/rooms/corridoio.webp';
import cucina from '../assets/rooms/cucina.webp';
import defaultRoom from '../assets/rooms/default-room.webp';
import salotto from '../assets/rooms/salotto.webp';

/**
 * Risoluzione centralizzata delle foto delle stanze.
 *
 * Usata sia dalla Home (RoomCard) sia dalla Room View (RoomHeader): nessun componente
 * deve hardcodare un'immagine. Qualsiasi nome non riconosciuto usa `default-room`.
 */

export type RoomImageKey =
  | 'salotto'
  | 'camera-da-letto'
  | 'cucina'
  | 'bagno-grande'
  | 'bagno-piccolo'
  | 'corridoio'
  | 'cameretta'
  | 'default-room';

/** Posizione del crop (CSS object-position) per breakpoint. Default: centro. */
export interface RoomImagePosition {
  desktop?: string;
  tablet?: string;
  mobile?: string;
}

export interface RoomImage {
  key: RoomImageKey;
  src: string;
  position: RoomImagePosition;
  /** true se la stanza non è una delle stanze ufficiali (fallback). */
  isFallback: boolean;
}

const SOURCES: Record<RoomImageKey, string> = {
  salotto,
  'camera-da-letto': cameraDaLetto,
  cucina,
  'bagno-grande': bagnoGrande,
  'bagno-piccolo': bagnoPiccolo,
  corridoio,
  cameretta,
  'default-room': defaultRoom,
};

/** Posizioni di crop opzionali per stanza: aggiustare qui se un breakpoint taglia male il soggetto. */
const POSITIONS: Partial<Record<RoomImageKey, RoomImagePosition>> = {
  salotto: { mobile: '35% 50%' },
  'camera-da-letto': { mobile: '60% 50%' },
  cucina: { mobile: '40% 50%' },
  'bagno-grande': { mobile: '50% 50%' },
  'bagno-piccolo': { mobile: '30% 50%' },
  corridoio: { mobile: '50% 50%' },
  cameretta: { mobile: '60% 50%' },
};

/** Nomi normalizzati (trim, minuscolo, senza accenti) → chiave immagine. */
const ALIASES: Record<string, RoomImageKey> = {
  salotto: 'salotto',
  soggiorno: 'salotto',
  'living room': 'salotto',
  living: 'salotto',
  'camera da letto': 'camera-da-letto',
  camera: 'camera-da-letto',
  bedroom: 'camera-da-letto',
  'master bedroom': 'camera-da-letto',
  cucina: 'cucina',
  kitchen: 'cucina',
  'bagno grande': 'bagno-grande',
  'bagno piccolo': 'bagno-piccolo',
  corridoio: 'corridoio',
  hallway: 'corridoio',
  corridor: 'corridoio',
  cameretta: 'cameretta',
};

/** Normalizza un nome di stanza: trim, minuscolo, accenti rimossi, spazi multipli ridotti. */
export function normalizeRoomName(name: string): string {
  return name
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .trim()
    .toLowerCase()
    .replace(/\s+/g, ' ');
}

/** Chiave immagine per un nome di stanza; `default-room` per nomi sconosciuti, vuoti o assenti. */
export function roomImageKeyFor(name: string | null | undefined): { key: RoomImageKey; isFallback: boolean } {
  if (!name) return { key: 'default-room', isFallback: true };
  const key = ALIASES[normalizeRoomName(name)];
  return key ? { key, isFallback: false } : { key: 'default-room', isFallback: true };
}

/** Immagine completa (sorgente, posizioni, flag di fallback) per una stanza. Non lancia mai. */
export function resolveRoomImage(name: string | null | undefined): RoomImage {
  const { key, isFallback } = roomImageKeyFor(name);
  return { key, src: SOURCES[key], position: POSITIONS[key] ?? {}, isFallback };
}

/** Fotografia effettiva di una stanza: personalizzata se presente, altrimenti quella predefinita. */
export interface RoomPhoto {
  src: string;
  position: RoomImagePosition;
  /** true se la foto è quella caricata dall'utente. */
  custom: boolean;
}

/**
 * Priorità: foto personalizzata (imageUrl dal backend, legata al roomId)
 *        → foto predefinita della stanza (resolver per nome)
 *        → default-room.webp (nomi sconosciuti).
 * Il nome serve solo per il fallback predefinito: la foto custom non dipende dal nome.
 */
export function resolveRoomPhoto(room: { name: string; imageUrl?: string | null }): RoomPhoto {
  if (room.imageUrl) {
    return { src: room.imageUrl, position: {}, custom: true };
  }
  const builtIn = resolveRoomImage(room.name);
  return { src: builtIn.src, position: builtIn.position, custom: false };
}
