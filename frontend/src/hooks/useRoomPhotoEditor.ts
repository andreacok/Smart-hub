import { useEffect, useRef, useState } from 'react';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import { resetRoomImage, uploadRoomImage } from '../api/roomImageApi';
import type { Home, RoomDetail, RoomImage, RoomSummary } from '../api/types';
import { useToast } from '../components/feedback/toastContext';
import { prepareRoomImage } from '../lib/roomImageFile';
import { queryKeys } from '../lib/queryKeys';

/** Foto scelta e preparata, in attesa di conferma. */
export interface PendingPhoto {
  blob: Blob;
  previewUrl: string;
}

/**
 * Flusso di cambio fotografia di una stanza: preparazione, anteprima, upload, ripristino.
 * La foto attuale resta visibile finché il nuovo upload non va a buon fine.
 */
export function useRoomPhotoEditor(room: RoomSummary, options: { onDone?: () => void } = {}) {
  const queryClient = useQueryClient();
  const toast = useToast();
  const [pending, setPending] = useState<PendingPhoto | null>(null);
  const [preparing, setPreparing] = useState(false);
  const [confirmReset, setConfirmReset] = useState(false);
  const pendingRef = useRef<PendingPhoto | null>(null);

  const replacePending = (next: PendingPhoto | null) => {
    if (pendingRef.current) URL.revokeObjectURL(pendingRef.current.previewUrl);
    pendingRef.current = next;
    setPending(next);
  };

  useEffect(() => () => {
    if (pendingRef.current) URL.revokeObjectURL(pendingRef.current.previewUrl);
  }, []);

  /** Aggiorna subito la foto nelle viste che la mostrano (Home e Room View), senza attendere il polling. */
  const applyServerState = (image: RoomImage) => {
    queryClient.setQueryData<RoomDetail>(queryKeys.room(room.id), (old) =>
      old && { ...old, room: { ...old.room, imageUrl: image.imageUrl, customImage: image.customImage } });
    queryClient.setQueryData<Home>(queryKeys.home, (old) =>
      old && {
        ...old,
        rooms: old.rooms.map((r) =>
          r.id === room.id ? { ...r, imageUrl: image.imageUrl, customImage: image.customImage } : r),
      });
  };

  const upload = useMutation<RoomImage, Error, PendingPhoto>({
    mutationFn: (photo) => uploadRoomImage(room.id, photo.blob, 'foto.jpg'),
    onSuccess: (image) => {
      applyServerState(image);
      replacePending(null);
      toast.show('Foto della stanza aggiornata', 'success');
      options.onDone?.();
    },
    // In caso di errore l'anteprima resta: l'utente può riprovare o annullare.
    onError: (error) => toast.show(error.message, 'error'),
  });

  const reset = useMutation<RoomImage, Error>({
    mutationFn: () => resetRoomImage(room.id),
    onSuccess: (image) => {
      applyServerState(image);
      setConfirmReset(false);
      toast.show('Foto predefinita ripristinata', 'success');
      options.onDone?.();
    },
    onError: (error) => toast.show(error.message, 'error'),
  });

  const busy = preparing || upload.isPending || reset.isPending;

  /** Prepara la foto scelta (HEIC, orientamento, ridimensionamento) e la mette in anteprima. */
  const choose = async (file: File) => {
    if (busy) return;
    setPreparing(true);
    try {
      const blob = await prepareRoomImage(file);
      replacePending({ blob, previewUrl: URL.createObjectURL(blob) });
    } catch (error) {
      toast.show(error instanceof Error ? error.message : 'Foto non valida', 'error');
    } finally {
      setPreparing(false);
    }
  };

  return {
    pending,
    preparing,
    uploading: upload.isPending,
    resetting: reset.isPending,
    busy,
    hasCustomImage: room.customImage === true,
    confirmReset,
    setConfirmReset,
    choose,
    discard: () => replacePending(null),
    confirm: () => {
      if (pendingRef.current && !busy) upload.mutate(pendingRef.current);
    },
    resetToDefault: () => {
      if (!busy) reset.mutate();
    },
  };
}

export type RoomPhotoEditor = ReturnType<typeof useRoomPhotoEditor>;
