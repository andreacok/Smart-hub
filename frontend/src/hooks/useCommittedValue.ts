import { useCallback, useEffect, useRef, useState } from 'react';

/**
 * Valore locale per slider e stepper: segue il valore del server, ma mentre l'utente
 * interagisce resta "suo" e viene inviato una sola volta dopo una breve pausa.
 * Evita che il polling faccia saltare lo slider durante il trascinamento.
 */
export function useCommittedValue(serverValue: number, commit: (value: number) => void, delayMs = 400) {
  const [value, setValue] = useState(serverValue);
  const editing = useRef(false);
  const timer = useRef<number | undefined>(undefined);
  const commitRef = useRef(commit);
  commitRef.current = commit;

  useEffect(() => {
    if (!editing.current) setValue(serverValue);
  }, [serverValue]);

  useEffect(() => () => window.clearTimeout(timer.current), []);

  const change = useCallback(
    (next: number) => {
      editing.current = true;
      setValue(next);
      window.clearTimeout(timer.current);
      timer.current = window.setTimeout(() => {
        editing.current = false;
        commitRef.current(next);
      }, delayMs);
    },
    [delayMs],
  );

  return [value, change] as const;
}
