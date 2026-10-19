import { request } from './client';
import type { RoomImage } from './types';

const enc = encodeURIComponent;

/** Carica la fotografia personalizzata di una stanza (multipart, il browser imposta il boundary). */
export function uploadRoomImage(roomId: string, file: Blob, fileName: string): Promise<RoomImage> {
  const form = new FormData();
  form.append('file', file, fileName);
  return request<RoomImage>(`/api/rooms/${enc(roomId)}/image`, { method: 'POST', body: form });
}

/** Ripristina la fotografia predefinita: elimina solo la personalizzata. */
export function resetRoomImage(roomId: string): Promise<RoomImage> {
  return request<RoomImage>(`/api/rooms/${enc(roomId)}/image`, { method: 'DELETE' });
}
