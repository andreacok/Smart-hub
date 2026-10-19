import { ApiError } from '../api/client';
import type { CommandType, Device } from '../api/types';

/**
 * Messaggi per l'utente, indicizzati dal codice applicativo. Il frontend decide il testo da qui:
 * lo status HTTP non compare mai nei messaggi.
 */
const MESSAGES: Record<string, string> = {
  SMARTTHINGS_UNAUTHORIZED: 'La connessione a SmartThings non è autorizzata. Verifica le credenziali configurate.',
  SMARTTHINGS_FORBIDDEN: 'SmartThings non consente questa operazione con i permessi attuali.',
  SMARTTHINGS_DEVICE_NOT_FOUND: 'Il dispositivo non è più disponibile in SmartThings.',
  SMARTTHINGS_DEVICE_OFFLINE: 'Il dispositivo non è raggiungibile tramite SmartThings.',
  SMARTTHINGS_CAPABILITY_NOT_SUPPORTED: 'Questo dispositivo non supporta questa funzione.',
  SMARTTHINGS_COMMAND_NOT_SUPPORTED: 'Questo dispositivo non supporta questo comando.',
  SMARTTHINGS_COMMAND_FAILED: 'SmartThings non è riuscito a eseguire il comando sul dispositivo.',
  SMARTTHINGS_RATE_LIMITED: 'Troppe richieste a SmartThings. Riprova tra poco.',
  SMARTTHINGS_TIMEOUT: 'SmartThings non ha risposto in tempo. Riprova.',
  SMARTTHINGS_UNAVAILABLE: 'SmartThings è temporaneamente non disponibile.',
  SMARTTHINGS_BAD_RESPONSE: 'SmartThings ha inviato una risposta non valida.',
  SMARTTHINGS_NETWORK_ERROR: 'Impossibile raggiungere SmartThings. Controlla la connessione.',
  SMARTTHINGS_ERROR: 'SmartThings ha restituito un errore.',
  SMART_HUB_VALIDATION_ERROR: 'La richiesta non è valida.',
  SMART_HUB_NOT_FOUND: 'Risorsa non trovata.',
  SMART_HUB_INTERNAL_ERROR: 'Si è verificato un errore inatteso.',
  SMART_HUB_UNAVAILABLE: 'Smart Hub non è raggiungibile in questo momento. Riprova tra poco.',
  NETWORK_UNREACHABLE: 'Impossibile raggiungere Smart Hub. Controlla la connessione.',
  NETWORK_TIMEOUT: 'Smart Hub non risponde. Riprova tra poco.',
};

const GENERIC_MESSAGE = 'Si è verificato un errore inatteso.';

/** Testo da mostrare per un errore qualsiasi. Mai "Failed to fetch", mai "Errore 502". */
export function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    const known = MESSAGES[error.code];
    if (known) return known;
    // Codice non ancora noto al frontend: il messaggio del backend è già pensato per l'utente.
    return error.message || GENERIC_MESSAGE;
  }
  return GENERIC_MESSAGE;
}

/** True quando Smart Hub o la rete non rispondono: utile per mostrare lo stato di connessione. */
export function isUnreachable(error: unknown): boolean {
  if (!(error instanceof ApiError)) return false;
  return error.status === 0 || error.code === 'SMART_HUB_UNAVAILABLE' || error.code.startsWith('NETWORK_');
}

const COMMAND_VERBS: Record<CommandType, string> = {
  TURN_ON: 'accendere',
  TURN_OFF: 'spegnere',
  SET_BRIGHTNESS: 'regolare la luminosità di',
  SET_COLOR_TEMPERATURE: 'regolare la temperatura colore di',
  SET_COLOR: 'cambiare il colore di',
  OPEN: 'aprire',
  CLOSE: 'chiudere',
  STOP: 'fermare',
  SET_POSITION: 'spostare',
  SET_TARGET_TEMPERATURE: 'impostare la temperatura di',
  SET_THERMOSTAT_MODE: 'cambiare la modalità di',
};

/** Messaggio di un comando fallito: contesto dell'azione più motivo. */
export function commandErrorMessage(device: Device, command: CommandType, error: unknown): string {
  return `Impossibile ${COMMAND_VERBS[command]} "${device.name}". ${errorMessage(error)}`;
}

/** Messaggio di un'azione su più dispositivi o una scena: contesto più motivo. */
export function actionErrorMessage(action: string, error: unknown): string {
  return `Impossibile ${action}. ${errorMessage(error)}`;
}
