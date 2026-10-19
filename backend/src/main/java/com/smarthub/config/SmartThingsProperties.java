package com.smarthub.config;

import com.smarthub.model.DeviceType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Configurazione dell'integrazione SmartThings.
 * Tutti i valori sono sovrascrivibili tramite environment variables (es. SMARTTHINGS_MODE, SMARTTHINGS_TOKEN).
 */
@ConfigurationProperties(prefix = "smartthings")
public class SmartThingsProperties {

    public enum Mode { MOCK, LIVE }

    public enum AuthType { PAT, OAUTH }

    /** mock = casa demo simulata, live = account SmartThings reale. */
    private Mode mode = Mode.MOCK;

    /** Base URL delle API SmartThings. */
    private String baseUrl = "https://api.smartthings.com/v1";

    /** Tipo di autenticazione in modalità live. */
    private AuthType auth = AuthType.PAT;

    /** Personal Access Token (solo auth=pat). Mai scriverlo nei sorgenti: usare SMARTTHINGS_TOKEN. */
    private String token = "";

    /** Location da usare. Se vuoto viene usata la prima location dell'account. */
    private String locationId = "";

    /** Timeout di ogni richiesta HTTP verso SmartThings. */
    private Duration requestTimeout = Duration.ofSeconds(10);

    /** Numero massimo di retry per le letture (errori 5xx, 429, timeout). */
    private int maxRetries = 2;

    /** Backoff iniziale tra i retry. */
    private Duration retryBackoff = Duration.ofMillis(300);

    /** Ogni quanto rileggere lo stato dei dispositivi (polling). */
    private Duration statusRefreshInterval = Duration.ofSeconds(5);

    /** Ogni quanto rileggere stanze, elenco dispositivi e scene. */
    private Duration structureRefreshInterval = Duration.ofMinutes(5);

    /** Richieste di stato in parallelo durante il refresh. */
    private int statusConcurrency = 4;

    /** Ogni quanto rileggere lo stato di salute (online/offline) dei dispositivi. */
    private Duration healthRefreshInterval = Duration.ofMinutes(1);

    /** Dopo un comando, attesa prima di rileggere lo stato reale del dispositivo. */
    private Duration commandRefreshDelay = Duration.ofMillis(1500);

    /**
     * Override manuale del tipo di dispositivo (deviceId -> tipo).
     * Utile ad esempio per un relè SmartThings "Switch" che in realtà comanda una luce.
     */
    private Map<String, DeviceType> deviceTypeOverrides = new LinkedHashMap<>();

    private final OAuth oauth = new OAuth();
    private final Mock mock = new Mock();

    public static class OAuth {
        private String clientId = "";
        private String clientSecret = "";
        /** Deve coincidere con uno dei redirect URI registrati nella OAuth-In SmartApp. */
        private String redirectUri = "";
        private String scopes = "r:locations:* r:devices:* x:devices:* r:scenes:* x:scenes:*";
        private String authorizeUrl = "https://api.smartthings.com/oauth/authorize";
        private String tokenUrl = "https://api.smartthings.com/oauth/token";
        /** Refresh token iniziale opzionale (es. ottenuto manualmente). */
        private String refreshToken = "";
        /** File in cui vengono salvati i token (il refresh token ruota a ogni utilizzo). */
        private String tokenStorePath = "./data/smartthings-oauth.json";

        public String getClientId() { return clientId; }
        public void setClientId(String clientId) { this.clientId = clientId; }
        public String getClientSecret() { return clientSecret; }
        public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
        public String getRedirectUri() { return redirectUri; }
        public void setRedirectUri(String redirectUri) { this.redirectUri = redirectUri; }
        public String getScopes() { return scopes; }
        public void setScopes(String scopes) { this.scopes = scopes; }
        public String getAuthorizeUrl() { return authorizeUrl; }
        public void setAuthorizeUrl(String authorizeUrl) { this.authorizeUrl = authorizeUrl; }
        public String getTokenUrl() { return tokenUrl; }
        public void setTokenUrl(String tokenUrl) { this.tokenUrl = tokenUrl; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public String getTokenStorePath() { return tokenStorePath; }
        public void setTokenStorePath(String tokenStorePath) { this.tokenStorePath = tokenStorePath; }
    }

    public static class Mock {
        /** Latenza simulata delle chiamate. */
        private Duration latency = Duration.ofMillis(120);
        /** Probabilità (0..1) che un comando fallisca: utile per provare il rollback ottimistico della UI. */
        private double commandFailureRate = 0.0;

        public Duration getLatency() { return latency; }
        public void setLatency(Duration latency) { this.latency = latency; }
        public double getCommandFailureRate() { return commandFailureRate; }
        public void setCommandFailureRate(double commandFailureRate) { this.commandFailureRate = commandFailureRate; }
    }

    public boolean isMock() { return mode == Mode.MOCK; }

    public Mode getMode() { return mode; }
    public void setMode(Mode mode) { this.mode = mode; }
    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public AuthType getAuth() { return auth; }
    public void setAuth(AuthType auth) { this.auth = auth; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getLocationId() { return locationId; }
    public void setLocationId(String locationId) { this.locationId = locationId; }
    public Duration getRequestTimeout() { return requestTimeout; }
    public void setRequestTimeout(Duration requestTimeout) { this.requestTimeout = requestTimeout; }
    public int getMaxRetries() { return maxRetries; }
    public void setMaxRetries(int maxRetries) { this.maxRetries = maxRetries; }
    public Duration getRetryBackoff() { return retryBackoff; }
    public void setRetryBackoff(Duration retryBackoff) { this.retryBackoff = retryBackoff; }
    public Duration getStatusRefreshInterval() { return statusRefreshInterval; }
    public void setStatusRefreshInterval(Duration statusRefreshInterval) { this.statusRefreshInterval = statusRefreshInterval; }
    public Duration getStructureRefreshInterval() { return structureRefreshInterval; }
    public void setStructureRefreshInterval(Duration structureRefreshInterval) { this.structureRefreshInterval = structureRefreshInterval; }
    public int getStatusConcurrency() { return statusConcurrency; }
    public void setStatusConcurrency(int statusConcurrency) { this.statusConcurrency = statusConcurrency; }
    public Duration getHealthRefreshInterval() { return healthRefreshInterval; }
    public void setHealthRefreshInterval(Duration healthRefreshInterval) { this.healthRefreshInterval = healthRefreshInterval; }
    public Duration getCommandRefreshDelay() { return commandRefreshDelay; }
    public void setCommandRefreshDelay(Duration commandRefreshDelay) { this.commandRefreshDelay = commandRefreshDelay; }
    public Map<String, DeviceType> getDeviceTypeOverrides() { return deviceTypeOverrides; }
    public void setDeviceTypeOverrides(Map<String, DeviceType> deviceTypeOverrides) { this.deviceTypeOverrides = deviceTypeOverrides; }
    public OAuth getOauth() { return oauth; }
    public Mock getMock() { return mock; }
}
