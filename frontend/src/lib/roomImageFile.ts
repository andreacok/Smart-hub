/**
 * Preparazione lato browser di una fotografia scelta dall'utente, prima dell'upload.
 *
 * - Valida dimensione e tipo (il tipo dichiarato dal browser è solo un indizio: il backend verifica il contenuto).
 * - Converte HEIC/HEIF (iPhone) in JPEG con heic2any, caricato solo quando serve.
 * - Applica l'orientamento EXIF, ridimensiona il lato lungo a 2400 px e ricodifica in JPEG:
 *   il canvas non porta con sé EXIF né GPS, quindi i metadati non lasciano il dispositivo.
 */

export const ROOM_IMAGE_MAX_BYTES = 10 * 1024 * 1024;
const LONG_EDGE_PX = 2400;
const JPEG_QUALITY = 0.9;

export type RoomImageErrorCode = 'EMPTY' | 'TOO_LARGE' | 'UNSUPPORTED' | 'CORRUPT' | 'PROCESSING';

export class RoomImageError extends Error {
  readonly code: RoomImageErrorCode;

  constructor(code: RoomImageErrorCode, message: string) {
    super(message);
    this.name = 'RoomImageError';
    this.code = code;
  }
}

const SUPPORTED_TYPES = new Set(['image/jpeg', 'image/jpg', 'image/png', 'image/webp', 'image/heic', 'image/heif']);
const HEIC_NAME = /\.(heic|heif)$/i;

export function isHeic(file: Pick<File, 'type' | 'name'>): boolean {
  return file.type === 'image/heic' || file.type === 'image/heif' || HEIC_NAME.test(file.name);
}

/** Controlli rapidi prima di qualunque elaborazione. Lancia RoomImageError con un messaggio per l'utente. */
export function validateRoomImageFile(file: Pick<File, 'size' | 'type' | 'name'>): void {
  if (file.size === 0) {
    throw new RoomImageError('EMPTY', 'Il file è vuoto.');
  }
  if (file.size > ROOM_IMAGE_MAX_BYTES) {
    throw new RoomImageError('TOO_LARGE', 'La foto supera il limite di 10 MB.');
  }
  // iOS e alcuni browser lasciano il tipo vuoto per gli HEIC: in quel caso decide l'estensione.
  const typeKnown = file.type === '' || SUPPORTED_TYPES.has(file.type);
  if (!typeKnown || (file.type === '' && !isHeic(file) && !/\.(jpe?g|png|webp)$/i.test(file.name))) {
    throw new RoomImageError('UNSUPPORTED', 'Formato non supportato: usa JPEG, PNG, WebP o HEIC.');
  }
}

/** Converte la fotografia in un JPEG pronto per il caricamento. */
export async function prepareRoomImage(file: File): Promise<Blob> {
  validateRoomImageFile(file);
  const source = isHeic(file) ? await convertHeic(file) : file;
  return reencodeAsJpeg(source);
}

async function convertHeic(file: File): Promise<Blob> {
  try {
    // Import dinamico: la libreria (~1 MB) si scarica solo se serve davvero.
    const { default: heic2any } = await import('heic2any');
    const output = await heic2any({ blob: file, toType: 'image/jpeg', quality: 0.92 });
    const first = Array.isArray(output) ? output[0] : output;
    if (!first) {
      throw new Error('HEIC senza immagini');
    }
    return first;
  } catch {
    throw new RoomImageError('UNSUPPORTED',
      'Non è stato possibile convertire questa foto. Prova a esportarla come JPEG dall\'app Foto.');
  }
}

async function reencodeAsJpeg(source: Blob): Promise<Blob> {
  let bitmap: ImageBitmap;
  try {
    bitmap = await createImageBitmap(source, { imageOrientation: 'from-image' });
  } catch {
    throw new RoomImageError('CORRUPT', 'Il file non sembra una foto valida.');
  }
  try {
    const scale = Math.min(1, LONG_EDGE_PX / Math.max(bitmap.width, bitmap.height));
    const width = Math.max(1, Math.round(bitmap.width * scale));
    const height = Math.max(1, Math.round(bitmap.height * scale));
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext('2d');
    if (!context) {
      throw new RoomImageError('PROCESSING', 'Non è stato possibile elaborare la foto. Riprova.');
    }
    // Sfondo bianco: i PNG trasparenti diventano JPEG leggibili.
    context.fillStyle = '#ffffff';
    context.fillRect(0, 0, width, height);
    context.drawImage(bitmap, 0, 0, width, height);
    const blob = await new Promise<Blob | null>((resolve) => canvas.toBlob(resolve, 'image/jpeg', JPEG_QUALITY));
    if (!blob) {
      throw new RoomImageError('PROCESSING', 'Non è stato possibile elaborare la foto. Riprova.');
    }
    return blob;
  } finally {
    bitmap.close();
  }
}
