import { useQuery } from '@tanstack/react-query';
import { api } from '../api/endpoints';
import { POLL_INTERVAL_MS, queryKeys } from '../lib/queryKeys';

/** Tutti i dispositivi della casa, aggiornati in polling. */
export function useDevices() {
  return useQuery({
    queryKey: queryKeys.devices,
    queryFn: ({ signal }) => api.devices(signal),
    refetchInterval: POLL_INTERVAL_MS,
  });
}
