package com.smarthub.smartthings;

import com.smarthub.config.SmartThingsProperties;
import com.smarthub.error.ApiErrorCode;
import com.smarthub.smartthings.api.SmartThingsException;
import com.smarthub.smartthings.api.StCommand;
import com.smarthub.smartthings.error.SmartThingsError;
import com.smarthub.smartthings.live.LiveSmartThingsApi;
import com.smarthub.smartthings.live.StaticTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Percorso reale del client SmartThings con risposte simulate: status, body, timeout e rete. */
class SmartThingsLiveErrorTest {

    private static final String SECRET_TOKEN = "pat-SECRET-token-value";

    private static LiveSmartThingsApi api(WebClient.Builder builder, Duration timeout) {
        SmartThingsProperties props = new SmartThingsProperties();
        props.setMaxRetries(0);
        props.setRequestTimeout(timeout);
        props.setRetryBackoff(Duration.ofMillis(1));
        return new LiveSmartThingsApi(builder.baseUrl("https://api.smartthings.test/v1").build(),
                new StaticTokenProvider(SECRET_TOKEN), props);
    }

    private static WebClient.Builder respondWith(HttpStatus status, String body, String retryAfter) {
        return WebClient.builder().exchangeFunction(request -> {
            ClientResponse.Builder response = ClientResponse.create(status)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body);
            if (retryAfter != null) {
                response.header("Retry-After", retryAfter);
            }
            return Mono.just(response.build());
        });
    }

    private static SmartThingsError failure(Runnable call) {
        SmartThingsException ex = assertThrows(SmartThingsException.class, call::run);
        return ex.error();
    }

    @Test
    void rateLimitIsClassifiedAndKeepsRetryAfter() {
        LiveSmartThingsApi api = api(respondWith(HttpStatus.TOO_MANY_REQUESTS, "", "30"), Duration.ofSeconds(2));
        SmartThingsError e = failure(() -> api.listDevices("loc-1"));
        assertEquals(ApiErrorCode.SMARTTHINGS_RATE_LIMITED, e.code());
        assertTrue(e.retryable());
        assertEquals(30L, e.retryAfterSeconds());
    }

    @Test
    void commandRejectionCarriesUpstreamRequestIdAndCode() {
        String body = "{\"requestId\":\"abc-123\",\"error\":{\"code\":\"ConstraintViolationError\","
                + "\"message\":\"Invalid argument\"}}";
        LiveSmartThingsApi api = api(respondWith(HttpStatus.UNPROCESSABLE_ENTITY, body, null), Duration.ofSeconds(2));
        SmartThingsError e = failure(() -> api.executeCommands("dev-9", List.of(
                new StCommand("main", "switch", "on", List.of()))));
        assertEquals(ApiErrorCode.SMARTTHINGS_COMMAND_FAILED, e.code());
        assertEquals("dev-9", e.deviceId());
        assertEquals("abc-123", e.upstreamRequestId());
        assertEquals("ConstraintViolationError", e.upstreamCode());
    }

    @Test
    void serverErrorIsUnavailable() {
        LiveSmartThingsApi api = api(respondWith(HttpStatus.SERVICE_UNAVAILABLE, "", null), Duration.ofSeconds(2));
        SmartThingsError e = failure(() -> api.listRooms("loc-1"));
        assertEquals(ApiErrorCode.SMARTTHINGS_UNAVAILABLE, e.code());
        assertTrue(e.retryable());
    }

    @Test
    void forbiddenIsNotRetryable() {
        LiveSmartThingsApi api = api(respondWith(HttpStatus.FORBIDDEN, "{\"error\":{\"message\":\"scope\"}}", null),
                Duration.ofSeconds(2));
        SmartThingsError e = failure(() -> api.listLocations());
        assertEquals(ApiErrorCode.SMARTTHINGS_FORBIDDEN, e.code());
        assertFalse(e.retryable());
    }

    @Test
    void unauthorizedAfterRetryIsUnauthorized() {
        LiveSmartThingsApi api = api(respondWith(HttpStatus.UNAUTHORIZED, "", null), Duration.ofSeconds(2));
        SmartThingsError e = failure(() -> api.listLocations());
        assertEquals(ApiErrorCode.SMARTTHINGS_UNAUTHORIZED, e.code());
    }

    @Test
    void slowResponseIsTimeout() {
        WebClient.Builder never = WebClient.builder().exchangeFunction(request -> Mono.never());
        LiveSmartThingsApi api = api(never, Duration.ofMillis(100));
        SmartThingsError e = failure(() -> api.listLocations());
        assertEquals(ApiErrorCode.SMARTTHINGS_TIMEOUT, e.code());
        assertTrue(e.retryable());
    }

    @Test
    void connectionFailureIsNetworkError() {
        WebClient.Builder broken = WebClient.builder().exchangeFunction(request ->
                Mono.error(new java.net.ConnectException("connection refused")));
        LiveSmartThingsApi api = api(broken, Duration.ofSeconds(2));
        SmartThingsError e = failure(() -> api.listLocations());
        assertEquals(ApiErrorCode.SMARTTHINGS_NETWORK_ERROR, e.code());
    }

    @Test
    void tokenNeverAppearsInAnyErrorField() {
        String body = "{\"requestId\":\"r1\",\"error\":{\"code\":\"X\",\"message\":\"echo " + SECRET_TOKEN + "\"}}";
        LiveSmartThingsApi api = api(respondWith(HttpStatus.BAD_REQUEST, body, null), Duration.ofSeconds(2));
        SmartThingsError e = failure(() -> api.listLocations());
        String everything = e.message() + " | " + e.detail() + " | " + e.upstreamCode() + " | " + e.upstreamRequestId();
        assertFalse(everything.contains(SECRET_TOKEN), everything);
    }
}
