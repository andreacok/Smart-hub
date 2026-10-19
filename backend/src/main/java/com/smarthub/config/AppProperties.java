package com.smarthub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/** Configurazione generale dell'applicazione. */
@ConfigurationProperties(prefix = "smarthub")
public class AppProperties {

    /** Origini autorizzate a chiamare le API (CORS). In Docker il frontend passa da Nginx, quindi non serve. */
    private List<String> corsAllowedOrigins = new ArrayList<>(List.of("http://localhost:5173", "http://127.0.0.1:5173"));

    /** Nome mostrato nel saluto della dashboard (opzionale). */
    private String ownerName = "";

    public List<String> getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public void setOwnerName(String ownerName) {
        this.ownerName = ownerName;
    }
}
