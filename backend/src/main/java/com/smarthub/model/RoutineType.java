package com.smarthub.model;

/**
 * Tipo di routine normalizzato. SmartThings le espone con due API distinte:
 * <ul>
 *     <li>{@link #MANUAL_ROUTINE}: scene (GET /scenes), senza trigger, eseguite a richiesta;</li>
 *     <li>{@link #AUTOMATION}: regole (GET /rules), con trigger, mostrate come automazioni nell'app.</li>
 * </ul>
 */
public enum RoutineType {
    MANUAL_ROUTINE,
    AUTOMATION
}
