package com.smarthub.smartthings.live;

import com.smarthub.config.SmartThingsProperties;
import com.smarthub.smartthings.api.SmartThingsApi;
import com.smarthub.smartthings.api.SmartThingsException;
import com.smarthub.smartthings.api.StCommand;
import com.smarthub.smartthings.api.StDevice;
import com.smarthub.smartthings.api.StDeviceStatus;
import com.smarthub.smartthings.api.StHealth;
import com.smarthub.smartthings.api.StLocation;
import com.smarthub.smartthings.api.StPage;
import com.smarthub.smartthings.api.StRoom;
import com.smarthub.smartthings.api.StScene;
import com.smarthub.smartthings.error.SmartThingsError;
import com.smarthub.smartthings.error.SmartThingsErrorMapper;
import com.smarthub.smartthings.error.SmartThingsOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

/**
 * Client HTTP verso le API ufficiali SmartThings (https://api.smartthings.com/v1).
 * <ul>
 *     <li>timeout per richiesta;</li>
 *     <li>retry con backoff esponenziale sulle letture (5xx, 429, timeout, errori di rete);</li>
 *     <li>su 401 il token viene invalidato e la richiesta ripetuta una volta (utile con OAuth);</li>
 *     <li>paginazione automatica tramite _links.next.</li>
 * </ul>
 */
public class LiveSmartThingsApi implements SmartThingsApi {

    private static final Logger log = LoggerFactory.getLogger(LiveSmartThingsApi.class);

    private static final ParameterizedTypeReference<StPage<StLocation>> LOCATIONS = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<StPage<StRoom>> ROOMS = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<StPage<StDevice>> DEVICES = new ParameterizedTypeReference<>() {
    };
    private static final ParameterizedTypeReference<StPage<StScene>> SCENES = new ParameterizedTypeReference<>() {
    };

    private final WebClient webClient;
    private final TokenProvider tokenProvider;
    private final Duration timeout;
    private final int maxRetries;
    private final Duration retryBackoff;

    public LiveSmartThingsApi(WebClient webClient, TokenProvider tokenProvider, SmartThingsProperties properties) {
        this.webClient = webClient;
        this.tokenProvider = tokenProvider;
        this.timeout = properties.getRequestTimeout();
        this.maxRetries = Math.max(0, properties.getMaxRetries());
        this.retryBackoff = properties.getRetryBackoff();
    }

    @Override
    public List<StLocation> listLocations() {
        return getAllPages("/locations", LOCATIONS);
    }

    @Override
    public List<StRoom> listRooms(String locationId) {
        return getAllPages("/locations/" + locationId + "/rooms", ROOMS);
    }

    @Override
    public List<StDevice> listDevices(String locationId) {
        return getAllPages("/devices?locationId=" + locationId, DEVICES);
    }

    @Override
    public StDeviceStatus getDeviceStatus(String deviceId) {
        return read(deviceId, "/devices/" + deviceId + "/status",
                client -> client.get().uri("/devices/{id}/status", deviceId)
                        .retrieve().bodyToMono(StDeviceStatus.class));
    }

    @Override
    public StHealth getDeviceHealth(String deviceId) {
        return read(deviceId, "/devices/" + deviceId + "/health",
                client -> client.get().uri("/devices/{id}/health", deviceId)
                        .retrieve().bodyToMono(StHealth.class));
    }

    @Override
    public void executeCommands(String deviceId, List<StCommand> commands) {
        log.debug("Comandi verso {}: {}", deviceId, commands);
        // I comandi NON vengono ripetuti automaticamente: un retry potrebbe eseguire due volte un'azione.
        execute(SmartThingsOperation.COMMAND, deviceId, "POST /devices/" + deviceId + "/commands", false,
                client -> client.post().uri("/devices/{id}/commands", deviceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("commands", commands))
                        .retrieve().toBodilessEntity());
    }

    @Override
    public List<StScene> listScenes(String locationId) {
        return getAllPages("/scenes?locationId=" + locationId, SCENES);
    }

    @Override
    public void executeScene(String sceneId) {
        execute(SmartThingsOperation.SCENE, null, "POST /scenes/" + sceneId + "/execute", false,
                client -> client.post().uri("/scenes/{id}/execute", sceneId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of())
                        .retrieve().toBodilessEntity());
    }

    // ------------------------------------------------------------ internals

    private <T> List<T> getAllPages(String path, ParameterizedTypeReference<StPage<T>> type) {
        List<T> result = new ArrayList<>();
        String next = path;
        int guard = 0;
        while (next != null && guard++ < 50) {
            final String target = next;
            StPage<T> page = read(null, target, client -> {
                if (target.startsWith("http")) {
                    return client.get().uri(URI.create(target)).retrieve().bodyToMono(type);
                }
                return client.get().uri(target).retrieve().bodyToMono(type);
            });
            if (page == null) {
                break;
            }
            if (page.items() != null) {
                result.addAll(page.items());
            }
            next = page.nextHref();
        }
        return result;
    }

    private <T> T read(String deviceId, String description, Function<WebClient, Mono<T>> call) {
        return execute(SmartThingsOperation.READ, deviceId, "GET " + description, true, call);
    }

    private <T> T execute(SmartThingsOperation operation, String deviceId, String description, boolean retryable,
                          Function<WebClient, Mono<T>> call) {
        try {
            return doExecute(retryable, call);
        } catch (WebClientResponseException.Unauthorized e) {
            log.warn("SmartThings ha risposto 401 per {}: ritento con un nuovo token", description);
            tokenProvider.invalidate();
            try {
                return doExecute(retryable, call);
            } catch (RuntimeException retryError) {
                throw translate(operation, deviceId, description, retryError);
            }
        } catch (RuntimeException e) {
            throw translate(operation, deviceId, description, e);
        }
    }

    private <T> T doExecute(boolean retryable, Function<WebClient, Mono<T>> call) {
        String token = tokenProvider.accessToken();
        WebClient authorized = webClient.mutate()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
        Mono<T> mono = Mono.defer(() -> call.apply(authorized)).timeout(timeout);
        if (retryable && maxRetries > 0) {
            mono = mono.retryWhen(Retry.backoff(maxRetries, retryBackoff)
                    .filter(LiveSmartThingsApi::isTransient)
                    .onRetryExhaustedThrow((spec, signal) -> signal.failure()));
        }
        return mono.block();
    }

    static boolean isTransient(Throwable t) {
        if (t instanceof WebClientResponseException r) {
            int status = r.getStatusCode().value();
            return status == 429 || status >= 500;
        }
        return t instanceof TimeoutException || t instanceof WebClientRequestException;
    }

    /** Converte qualsiasi errore di comunicazione nell'errore classificato dal mapper. Nessun body grezzo viene loggato. */
    private SmartThingsException translate(SmartThingsOperation operation, String deviceId, String description,
                                           Throwable e) {
        if (e instanceof SmartThingsException st) {
            return st;
        }
        Throwable cause = unwrap(e);
        log.debug("Errore SmartThings su {}: {}", description, cause.getClass().getSimpleName());
        SmartThingsError error = SmartThingsErrorMapper.fromException(operation, deviceId, cause);
        return new SmartThingsException(SmartThingsErrorMapper.redact(error, tokenProvider.secrets()), cause);
    }

    private static Throwable unwrap(Throwable e) {
        Throwable current = e;
        // Reactor incapsula le eccezioni checked (es. TimeoutException) in ReactiveException.
        while (current.getCause() != null && current.getCause() != current
                && !(current instanceof WebClientResponseException)
                && !(current instanceof TimeoutException)) {
            if (current.getClass().getName().startsWith("reactor.")) {
                current = current.getCause();
            } else {
                break;
            }
        }
        return current;
    }
}
