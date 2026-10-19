package com.smarthub.smartthings.live;

import java.util.List;
import java.util.ArrayList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthub.config.SmartThingsProperties;
import com.smarthub.smartthings.api.SmartThingsException;
import com.smarthub.smartthings.error.SmartThingsErrorMapper;
import com.smarthub.smartthings.error.SmartThingsOperation;
import com.smarthub.error.ApiErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;

/**
 * Gestione token OAuth 2.0 (authorization code) per una OAuth-In SmartApp.
 * <p>
 * I Personal Access Token SmartThings creati dopo il 30/12/2024 durano 24 ore, quindi per un hub
 * sempre acceso serve OAuth: l'access token (~24h) viene rinnovato automaticamente con il refresh token.
 * Il refresh token ruota a ogni utilizzo, per questo viene salvato su file (montare un volume in Docker).
 */
public class OAuthTokenProvider implements TokenProvider {

    private static final Logger log = LoggerFactory.getLogger(OAuthTokenProvider.class);
    private static final Duration EXPIRY_MARGIN = Duration.ofMinutes(5);

    /** Token persistiti. */
    public record StoredTokens(String accessToken, String refreshToken, Instant expiresAt) {
    }

    private final SmartThingsProperties.OAuth config;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final Duration timeout;
    private final Path storePath;

    private volatile StoredTokens tokens;

    public OAuthTokenProvider(SmartThingsProperties.OAuth config, WebClient webClient,
                              ObjectMapper objectMapper, Duration timeout) {
        this.config = config;
        this.webClient = webClient;
        this.objectMapper = objectMapper;
        this.timeout = timeout;
        this.storePath = Path.of(config.getTokenStorePath());
        this.tokens = load();
        if (tokens == null && !config.getRefreshToken().isBlank()) {
            tokens = new StoredTokens(null, config.getRefreshToken().trim(), Instant.EPOCH);
        }
    }

    @Override
    public List<String> secrets() {
        StoredTokens current = tokens;
        if (current == null) {
            return List.of();
        }
        List<String> secrets = new ArrayList<>();
        if (current.accessToken() != null) {
            secrets.add(current.accessToken());
        }
        if (current.refreshToken() != null) {
            secrets.add(current.refreshToken());
        }
        return secrets;
    }

    @Override
    public synchronized String accessToken() {
        StoredTokens current = tokens;
        if (current == null || current.refreshToken() == null) {
            throw new SmartThingsException(SmartThingsErrorMapper.custom(SmartThingsOperation.AUTH,
                    ApiErrorCode.SMARTTHINGS_UNAUTHORIZED, null,
                    "Autorizzazione OAuth mancante: avvia il login da /api/auth/smartthings/login"));
        }
        if (current.accessToken() != null && current.expiresAt().isAfter(Instant.now().plus(EXPIRY_MARGIN))) {
            return current.accessToken();
        }
        return refresh().accessToken();
    }

    @Override
    public synchronized void invalidate() {
        StoredTokens current = tokens;
        if (current != null) {
            tokens = new StoredTokens(current.accessToken(), current.refreshToken(), Instant.EPOCH);
        }
    }

    @Override
    public boolean isConfigured() {
        return !config.getClientId().isBlank() && !config.getClientSecret().isBlank();
    }

    public boolean isAuthorized() {
        StoredTokens current = tokens;
        return current != null && current.refreshToken() != null;
    }

    public Instant accessTokenExpiry() {
        StoredTokens current = tokens;
        return current == null ? null : current.expiresAt();
    }

    @Override
    public String describe() {
        if (!isConfigured()) {
            return "OAuth (client id/secret mancanti)";
        }
        return isAuthorized() ? "OAuth (autorizzato)" : "OAuth (in attesa di autorizzazione)";
    }

    /** URL da aprire nel browser per autorizzare l'app sul proprio account SmartThings. */
    public String authorizationUrl(String state) {
        return config.getAuthorizeUrl()
                + "?client_id=" + encode(config.getClientId())
                + "&response_type=code"
                + "&redirect_uri=" + encode(config.getRedirectUri())
                + "&scope=" + encode(config.getScopes())
                + "&state=" + encode(state);
    }

    /** Scambia il codice di autorizzazione con access + refresh token. */
    public synchronized void exchangeCode(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", config.getClientId());
        form.add("redirect_uri", config.getRedirectUri());
        requestTokens(form);
        log.info("Autorizzazione SmartThings completata, token salvati in {}", storePath.toAbsolutePath());
    }

    private StoredTokens refresh() {
        log.info("Rinnovo access token SmartThings");
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "refresh_token");
        form.add("refresh_token", tokens.refreshToken());
        form.add("client_id", config.getClientId());
        return requestTokens(form);
    }

    private StoredTokens requestTokens(MultiValueMap<String, String> form) {
        String basic = Base64.getEncoder().encodeToString(
                (config.getClientId() + ":" + config.getClientSecret()).getBytes(StandardCharsets.UTF_8));
        Map<String, Object> body;
        try {
            body = webClient.post()
                    .uri(config.getTokenUrl())
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromFormData(form))
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                    })
                    .timeout(timeout)
                    .block();
        } catch (RuntimeException e) {
            // Il body di SmartThings viene classificato dal mapper e mai inoltrato grezzo.
            throw new SmartThingsException(
                    SmartThingsErrorMapper.redact(SmartThingsErrorMapper.fromException(SmartThingsOperation.AUTH, null, e), secrets()), e);
        }
        if (body == null || body.get("access_token") == null) {
            throw new SmartThingsException(SmartThingsErrorMapper.badResponse(SmartThingsOperation.AUTH, null,
                    "Risposta del token OAuth senza access_token"));
        }
        String access = body.get("access_token").toString();
        Object refreshValue = body.get("refresh_token");
        String refresh = refreshValue != null ? refreshValue.toString()
                : (tokens != null ? tokens.refreshToken() : null);
        long expiresIn = body.get("expires_in") instanceof Number n ? n.longValue() : 86_399L;
        StoredTokens updated = new StoredTokens(access, refresh, Instant.now().plusSeconds(expiresIn));
        tokens = updated;
        save(updated);
        return updated;
    }

    private StoredTokens load() {
        if (!Files.exists(storePath)) {
            return null;
        }
        try {
            StoredTokens stored = objectMapper.readValue(storePath.toFile(), StoredTokens.class);
            log.info("Token OAuth SmartThings caricati da {}", storePath.toAbsolutePath());
            return stored;
        } catch (IOException e) {
            log.warn("Impossibile leggere {}: {}", storePath.toAbsolutePath(), e.getMessage());
            return null;
        }
    }

    private void save(StoredTokens value) {
        try {
            Path parent = storePath.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = storePath.resolveSibling(storePath.getFileName() + ".tmp");
            objectMapper.writeValue(tmp.toFile(), value);
            Files.move(tmp, storePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            log.error("Impossibile salvare i token OAuth in {}: {}", storePath.toAbsolutePath(), e.getMessage());
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
