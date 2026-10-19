package com.smarthub.smartthings.live;

import java.util.List;

/** Fornisce l'access token da usare verso SmartThings. Il token non esce mai dal backend. */
public interface TokenProvider {

    /** Access token valido. Lancia SmartThingsException se non disponibile. */
    String accessToken();

    /** Chiamato quando SmartThings risponde 401: il provider può forzare un refresh. */
    /** Segreti da rimuovere da ogni errore prima che esca dal backend. Non deve fare chiamate di rete. */
    default List<String> secrets() {
        return List.of();
    }

    default void invalidate() {
    }

    /** true se il provider è in grado di fornire un token (configurazione presente). */
    boolean isConfigured();

    /** Descrizione sintetica, senza segreti, per l'endpoint di stato. */
    String describe();
}
