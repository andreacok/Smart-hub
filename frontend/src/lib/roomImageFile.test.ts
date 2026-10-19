import { describe, expect, it } from 'vitest';
import { ROOM_IMAGE_MAX_BYTES, RoomImageError, isHeic, validateRoomImageFile } from './roomImageFile';

const file = (overrides: Partial<{ size: number; type: string; name: string }> = {}) => ({
  size: 1024,
  type: 'image/jpeg',
  name: 'foto.jpg',
  ...overrides,
});

function expectCode(fn: () => void, code: string) {
  try {
    fn();
  } catch (error) {
    expect(error).toBeInstanceOf(RoomImageError);
    expect((error as RoomImageError).code).toBe(code);
    return;
  }
  throw new Error(`atteso errore ${code}`);
}

describe('validateRoomImageFile', () => {
  it('accetta JPEG, PNG, WebP e HEIC', () => {
    for (const type of ['image/jpeg', 'image/png', 'image/webp', 'image/heic', 'image/heif']) {
      expect(() => validateRoomImageFile(file({ type }))).not.toThrow();
    }
  });

  it('accetta un HEIC di iOS che arriva senza tipo, riconosciuto dall\'estensione', () => {
    expect(() => validateRoomImageFile(file({ type: '', name: 'IMG_0001.HEIC' }))).not.toThrow();
  });

  it('rifiuta i file vuoti', () => {
    expectCode(() => validateRoomImageFile(file({ size: 0 })), 'EMPTY');
  });

  it('rifiuta i file oltre 10 MB con un messaggio chiaro', () => {
    expect(ROOM_IMAGE_MAX_BYTES).toBe(10 * 1024 * 1024);
    expectCode(() => validateRoomImageFile(file({ size: ROOM_IMAGE_MAX_BYTES + 1 })), 'TOO_LARGE');
  });

  it('rifiuta i formati non supportati', () => {
    expectCode(() => validateRoomImageFile(file({ type: 'application/pdf', name: 'doc.pdf' })), 'UNSUPPORTED');
    expectCode(() => validateRoomImageFile(file({ type: 'image/gif', name: 'anim.gif' })), 'UNSUPPORTED');
  });

  it('riconosce HEIC per tipo o estensione', () => {
    expect(isHeic({ type: 'image/heic', name: 'a' })).toBe(true);
    expect(isHeic({ type: '', name: 'IMG.heif' })).toBe(true);
    expect(isHeic({ type: 'image/jpeg', name: 'a.jpg' })).toBe(false);
  });
});
