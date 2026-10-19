import { useMutation, useQueryClient } from '@tanstack/react-query';
import { api } from '../api/endpoints';
import { commandErrorMessage } from '../lib/apiErrors';
import type { Device, DeviceCommand, Home, RoomDetail } from '../api/types';
import { useToast } from '../components/feedback/toastContext';
import { predictDevice } from '../lib/predict';
import { queryKeys } from '../lib/queryKeys';
import { replaceDeviceInHome, replaceDeviceInRoom } from './cacheUpdates';

interface Variables {
  device: Device;
  command: DeviceCommand;
}

interface Snapshot {
  home: Home | undefined;
  room: RoomDetail | undefined;
}

/**
 * Invio comandi con aggiornamento ottimistico:
 * la UI cambia subito, se il comando fallisce lo stato precedente viene ripristinato
 * e viene mostrato un messaggio non invasivo.
 */
export function useDeviceCommand() {
  const queryClient = useQueryClient();
  const toast = useToast();

  return useMutation<Device, Error, Variables, Snapshot>({
    mutationFn: ({ device, command }) => api.sendCommand(device.id, command),

    onMutate: async ({ device, command }) => {
      const roomKey = queryKeys.room(device.roomId);
      await Promise.all([
        queryClient.cancelQueries({ queryKey: queryKeys.home }),
        queryClient.cancelQueries({ queryKey: roomKey }),
      ]);
      const snapshot: Snapshot = {
        home: queryClient.getQueryData<Home>(queryKeys.home),
        room: queryClient.getQueryData<RoomDetail>(roomKey),
      };
      const predicted = predictDevice(device, command);
      queryClient.setQueryData<RoomDetail>(roomKey, (room) => replaceDeviceInRoom(room, predicted));
      queryClient.setQueryData<Home>(queryKeys.home, (home) => replaceDeviceInHome(home, device, predicted));
      return snapshot;
    },

    onError: (error, { device, command }, snapshot) => {
      if (snapshot) {
        queryClient.setQueryData(queryKeys.home, snapshot.home);
        queryClient.setQueryData(queryKeys.room(device.roomId), snapshot.room);
      }
      toast.show(commandErrorMessage(device, command.command, error), 'error');
    },

    onSuccess: (updated, { device }) => {
      queryClient.setQueryData<RoomDetail>(queryKeys.room(device.roomId), (room) => replaceDeviceInRoom(room, updated));
    },

    onSettled: (_data, _error, { device }) => {
      void queryClient.invalidateQueries({ queryKey: queryKeys.room(device.roomId) });
      void queryClient.invalidateQueries({ queryKey: queryKeys.home });
    },
  });
}
