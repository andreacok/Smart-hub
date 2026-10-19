import { Camera, Image as ImageIcon, Pencil, RotateCcw, Upload, X } from 'lucide-react';
import { useEffect, useId, useRef } from 'react';
import type { ChangeEvent } from 'react';
import { createPortal } from 'react-dom';
import { useMediaQuery } from '../../hooks/useMediaQuery';
import type { RoomPhotoEditor as Editor } from '../../hooks/useRoomPhotoEditor';
import styles from './RoomPhotoEditor.module.css';

interface RoomPhotoEditorProps {
  open: boolean;
  roomName: string;
  editor: Editor;
  onClose: () => void;
}

/**
 * Foglio per cambiare la fotografia della stanza.
 * Mobile: bottom sheet con "Scatta foto" (fotocamera posteriore) e "Scegli dalla galleria".
 * Desktop/tablet: dialog centrale con "Carica immagine".
 */
export function RoomPhotoEditor({ open, roomName, editor, onClose }: RoomPhotoEditorProps) {
  const compact = useMediaQuery('(max-width: 760px)');
  const cameraInput = useRef<HTMLInputElement>(null);
  const galleryInput = useRef<HTMLInputElement>(null);
  const titleId = useId();

  useEffect(() => {
    if (!open) return undefined;
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !editor.busy) onClose();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, onClose, editor.busy]);

  if (!open) return null;

  const onFile = (event: ChangeEvent<HTMLInputElement>) => {
    const file = event.target.files?.[0];
    // Svuota l'input: così la stessa foto può essere riscelta dopo un annullamento.
    event.target.value = '';
    if (file) void editor.choose(file);
  };

  const close = () => {
    if (!editor.busy) onClose();
  };

  return createPortal(
    <div className={styles.backdrop} onClick={close}>
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby={titleId}
        className={styles.sheet}
        onClick={(event) => event.stopPropagation()}
      >
        <span className={styles.grabber} aria-hidden="true" />

        <header className={styles.head}>
          <span className={styles.headIcon} aria-hidden="true"><Pencil size={20} /></span>
          <div className={styles.headText}>
            <h2 id={titleId} className={styles.title}>Foto della stanza</h2>
            <p className={styles.subtitle}>{roomName}</p>
          </div>
          <button type="button" className={styles.close} onClick={close} disabled={editor.busy} aria-label="Chiudi">
            <X size={22} aria-hidden="true" />
          </button>
        </header>

        {editor.pending ? (
          <div className={styles.preview}>
            <img className={styles.previewImage} src={editor.pending.previewUrl} alt="Anteprima della nuova foto" />
            <div className={styles.previewActions}>
              <button
                type="button"
                className={styles.primary}
                onClick={editor.confirm}
                disabled={editor.busy}
                aria-busy={editor.uploading}
              >
                {editor.uploading ? (
                  <>
                    <span className={styles.spinner} aria-hidden="true" />
                    Caricamento…
                  </>
                ) : (
                  'Usa questa foto'
                )}
              </button>
              <button type="button" className={styles.secondary} onClick={editor.discard} disabled={editor.busy}>
                Annulla
              </button>
            </div>
          </div>
        ) : (
          <ul className={styles.options}>
            {compact && (
              <li>
                <button type="button" className={styles.option} onClick={() => cameraInput.current?.click()} disabled={editor.busy}>
                  <span className={styles.optionIcon} aria-hidden="true"><Camera size={22} /></span>
                  Scatta foto
                </button>
              </li>
            )}
            <li>
              <button type="button" className={styles.option} onClick={() => galleryInput.current?.click()} disabled={editor.busy}>
                <span className={styles.optionIcon} aria-hidden="true">
                  {compact ? <ImageIcon size={22} /> : <Upload size={22} />}
                </span>
                {compact ? 'Scegli dalla galleria' : 'Carica immagine'}
              </button>
            </li>

            {editor.hasCustomImage && !editor.confirmReset && (
              <li>
                <button type="button" className={styles.option} onClick={() => editor.setConfirmReset(true)} disabled={editor.busy}>
                  <span className={styles.optionIcon} aria-hidden="true"><RotateCcw size={22} /></span>
                  Ripristina foto predefinita
                </button>
              </li>
            )}
            {editor.hasCustomImage && editor.confirmReset && (
              <li className={styles.confirm}>
                <p className={styles.confirmText}>Tornare alla foto predefinita?</p>
                <div className={styles.confirmActions}>
                  <button type="button" className={styles.danger} onClick={editor.resetToDefault} disabled={editor.busy}>
                    {editor.resetting ? 'Ripristino…' : 'Ripristina'}
                  </button>
                  <button type="button" className={styles.secondary} onClick={() => editor.setConfirmReset(false)} disabled={editor.busy}>
                    Annulla
                  </button>
                </div>
              </li>
            )}

            {editor.preparing && (
              <li className={styles.status} role="status">
                <span className={styles.spinner} aria-hidden="true" />
                Preparazione della foto…
              </li>
            )}
          </ul>
        )}

        <input
          ref={cameraInput}
          type="file"
          accept="image/*"
          capture="environment"
          className={styles.fileInput}
          onChange={onFile}
          tabIndex={-1}
          aria-hidden="true"
        />
        <input
          ref={galleryInput}
          type="file"
          accept="image/*"
          className={styles.fileInput}
          onChange={onFile}
          tabIndex={-1}
          aria-hidden="true"
        />
      </div>
    </div>,
    document.body,
  );
}
