package com.smarthub.error;

/** Da dove nasce un errore: serve a capire se il problema è di SmartThings, di Smart Hub, della rete o ignoto. */
public enum ErrorSource {
    SMARTTHINGS,
    SMART_HUB,
    NETWORK,
    UNKNOWN
}
