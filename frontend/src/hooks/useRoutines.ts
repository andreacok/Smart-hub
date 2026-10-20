import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/endpoints';
import type { Routine } from '../api/types';
import { useToast } from '../components/feedback/toastContext';
import { actionErrorMessage } from '../lib/apiErrors';
import { queryKeys } from '../lib/queryKeys';

/**
 * Routine e automazioni della location. Letta da SmartThings ogni volta che il tab viene aperto,
 * senza polling in background: il tab resta fermo finché l'utente non lo riapre.
 */
export function useRoutines() {
  return useQuery({
    queryKey: queryKeys.routines,
    queryFn: ({ signal }) => api.routines(signal),
  });
}

/** Esecuzione reale di una routine o automazione tramite il backend. Nessuna simulazione lato client. */
export function useExecuteRoutine() {
  const queryClient = useQueryClient();
  const toast = useToast();
  return useMutation<void, Error, Routine>({
    mutationFn: (routine) => api.executeRoutine(routine.id),
    onSuccess: (_data, routine) => toast.show(`"${routine.name}" avviata`, 'success'),
    onError: (error, routine) => toast.show(actionErrorMessage(`avviare "${routine.name}"`, error), 'error'),
    onSettled: () => queryClient.invalidateQueries(),
  });
}
