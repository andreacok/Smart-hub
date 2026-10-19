import { useQuery } from '@tanstack/react-query';
import { api } from '../api/endpoints';
import { ApiError } from '../api/client';
import { POLL_INTERVAL_MS, queryKeys } from '../lib/queryKeys';

/** Dettaglio stanza con i dispositivi, aggiornato in polling. */
export function useRoom(roomId: string) {
  return useQuery({
    queryKey: queryKeys.room(roomId),
    queryFn: ({ signal }) => api.room(roomId, signal),
    refetchInterval: (query) =>
      query.state.error instanceof ApiError && query.state.error.status === 404 ? false : POLL_INTERVAL_MS,
    retry: (count, error) => !(error instanceof ApiError && error.status === 404) && count < 2,
  });
}
