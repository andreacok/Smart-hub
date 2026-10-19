package com.smarthub.controller;

import com.smarthub.exception.BadRequestException;
import com.smarthub.service.HomeStateCache;
import com.smarthub.smartthings.live.OAuthTokenProvider;
import com.smarthub.smartthings.live.TokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Flusso OAuth per collegare l'account SmartThings (solo con SMARTTHINGS_AUTH=oauth).
 * <ol>
 *     <li>GET /api/auth/smartthings/login → redirect alla pagina di consenso SmartThings;</li>
 *     <li>SmartThings richiama il redirect URI con ?code=...: se punta a /api/auth/smartthings/callback
 *     lo scambio è automatico, altrimenti si invia il codice a POST /api/auth/smartthings/code.</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/auth/smartthings")
public class SmartThingsAuthController {

    private static final Logger log = LoggerFactory.getLogger(SmartThingsAuthController.class);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final TokenProvider tokenProvider;
    private final HomeStateCache cache;
    private final Set<String> pendingStates = ConcurrentHashMap.newKeySet();

    public SmartThingsAuthController(TokenProvider tokenProvider, HomeStateCache cache) {
        this.tokenProvider = tokenProvider;
        this.cache = cache;
    }

    @GetMapping("/status")
    public Map<String, Object> status() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("auth", tokenProvider.describe());
        result.put("configured", tokenProvider.isConfigured());
        if (tokenProvider instanceof OAuthTokenProvider oauth) {
            result.put("authorized", oauth.isAuthorized());
            result.put("accessTokenExpiry", oauth.accessTokenExpiry());
        }
        return result;
    }

    @GetMapping("/login")
    public ResponseEntity<Void> login() {
        OAuthTokenProvider oauth = requireOAuth();
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        String state = HexFormat.of().formatHex(bytes);
        pendingStates.add(state);
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(oauth.authorizationUrl(state)))
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<String> callback(@RequestParam String code,
                                           @RequestParam(required = false) String state) {
        OAuthTokenProvider oauth = requireOAuth();
        if (state != null && !pendingStates.remove(state)) {
            throw new BadRequestException("Parametro state non valido: ripeti il login");
        }
        oauth.exchangeCode(code);
        reloadQuietly();
        String html = "<!doctype html><meta charset=utf-8><title>Smart Hub</title>"
                + "<body style=\"font-family:system-ui;background:#0f1115;color:#f2f4f8;padding:48px\">"
                + "<h1>SmartThings collegato</h1><p>Puoi chiudere questa pagina e tornare alla dashboard.</p></body>";
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE).body(html);
    }

    /** Alternativa manuale: incolla qui il "code" ricevuto sul redirect URI. Body: {"code":"..."} */
    @PostMapping("/code")
    public Map<String, Object> code(@RequestBody Map<String, String> body) {
        OAuthTokenProvider oauth = requireOAuth();
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Campo 'code' mancante");
        }
        oauth.exchangeCode(code.trim());
        reloadQuietly();
        return status();
    }

    private void reloadQuietly() {
        try {
            cache.refreshStructure();
            cache.refreshStatuses(true);
        } catch (RuntimeException e) {
            log.warn("Token ottenuto ma il caricamento dei dati è fallito: {}", e.getMessage());
        }
    }

    private OAuthTokenProvider requireOAuth() {
        if (tokenProvider instanceof OAuthTokenProvider oauth) {
            return oauth;
        }
        throw new BadRequestException("Disponibile solo con SMARTTHINGS_MODE=live e SMARTTHINGS_AUTH=oauth");
    }
}
