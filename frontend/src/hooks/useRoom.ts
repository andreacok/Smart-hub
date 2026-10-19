import { useQuery } from '@tanstack/react-query';
import { api } from '../api/endpoints';
import { ApiError } from '../api/client';
import type { RoomDetail } from '../api/types';
import { POLL_INTERVAL_MS, queryKeys } from '../lib/queryKeys';

const FAST_POLL_MS = 1000;

function somethingMoving(room: RoomDetail | undefined): boolean {
  return !!room?.devices.some((d) => d.state.openState === 'OPENING' || d.state.openState === 'CLOSING');
}

/** Dettaglio stanza con i dispositivi, aggiornato in polling (più rapido mentre una tapparella si muove). */
export function useRoom(roomId: string) {
  return useQuery({
    queryKey: queryKeys.room(roomId),
    queryFn: ({ signal }) => api.room(roomId, signal),
    refetchInterval: (query) => {
      if (query.state.error instanceof ApiError && query.state.error.status === 404) return false;
      return somethingMoving(query.state.data) ? FAST_POLL_MS : POLL_INTERVAL_MS;
    },
    retry: (count, error) => !(error instanceof ApiError && error.status === 404) && count < 2,
  });
}
