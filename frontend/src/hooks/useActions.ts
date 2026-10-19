import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/endpoints';
import type { ActionResult, Scene } from '../api/types';
import { useToast } from '../components/feedback/toastContext';

function reportResult(result: ActionResult, toast: ReturnType<typeof useToast>, emptyMessage: string) {
  if (result.failed > 0) {
    toast.show(`${result.failed} dispositivi non hanno risposto`, 'error');
  } else if (result.succeeded === 0) {
    toast.show(emptyMessage);
  }
}

/** Esecuzione di una scena SmartThings. */
export function useExecuteScene() {
  const queryClient = useQueryClient();
  const toast = useToast();
  return useMutation<void, Error, Scene>({
    mutationFn: (scene) => api.executeScene(scene.id),
    onSuccess: (_data, scene) => toast.show(`Scena "${scene.name}" avviata`, 'success'),
    onError: (error, scene) => toast.show(`${scene.name}: ${error.message}`, 'error'),
    onSettled: () => queryClient.invalidateQueries(),
  });
}

/** Spegne luci e prese accese di una stanza. */
export function useRoomAllOff(roomId: string) {
  const queryClient = useQueryClient();
  const toast = useToast();
  return useMutation<ActionResult, Error>({
    mutationFn: () => api.roomAllOff(roomId),
    onSuccess: (result) => reportResult(result, toast, 'In questa stanza è già tutto spento'),
    onError: (error) => toast.show(error.message, 'error'),
    onSettled: () => queryClient.invalidateQueries(),
  });
}

/** Spegne tutte le luci della casa. */
export function useHomeLightsOff() {
  const queryClient = useQueryClient();
  const toast = useToast();
  return useMutation<ActionResult, Error>({
    mutationFn: () => api.homeLightsOff(),
    onSuccess: (result) => reportResult(result, toast, 'Le luci sono già tutte spente'),
    onError: (error) => toast.show(error.message, 'error'),
    onSettled: () => queryClient.invalidateQueries(),
  });
}
