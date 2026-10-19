import { useQuery } from '@tanstack/react-query';
import { api } from '../api/endpoints';
import { POLL_INTERVAL_MS, queryKeys } from '../lib/queryKeys';

/** Stato del sistema (modalità, connessione, ultimi aggiornamenti), per la pagina "Altro". */
export function useSystemStatus() {
  return useQuery({
    queryKey: queryKeys.system,
    queryFn: ({ signal }) => api.systemStatus(signal),
    refetchInterval: POLL_INTERVAL_MS * 5,
  });
}
