import { useQuery } from '@tanstack/react-query';
import { api } from '../api/endpoints';
import { POLL_INTERVAL_MS, queryKeys } from '../lib/queryKeys';

/** Dati della home dashboard, aggiornati in polling. */
export function useHome() {
  return useQuery({
    queryKey: queryKeys.home,
    queryFn: ({ signal }) => api.home(signal),
    refetchInterval: POLL_INTERVAL_MS,
  });
}
