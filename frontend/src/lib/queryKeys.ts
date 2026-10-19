export const queryKeys = {
  home: ['home'] as const,
  room: (roomId: string) => ['room', roomId] as const,
  rooms: ['rooms'] as const,
  devices: ['devices'] as const,
  system: ['system'] as const,
};

/** Intervallo di polling dello stato (ms). Configurabile con VITE_POLL_INTERVAL_MS. */
export const POLL_INTERVAL_MS = Number(import.meta.env.VITE_POLL_INTERVAL_MS ?? 3000);
