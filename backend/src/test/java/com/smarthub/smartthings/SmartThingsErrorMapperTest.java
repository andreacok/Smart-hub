package com.smarthub.smartthings;

import com.smarthub.error.ApiErrorCode;
import com.smarthub.error.ErrorSource;
import com.smarthub.smartthings.error.SmartThingsError;
import com.smarthub.smartthings.error.SmartThingsErrorMapper;
import com.smarthub.smartthings.error.SmartThingsOperation;
import org.junit.jupiter.api.Test;
import org.springframework.core.codec.DecodingException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartThingsErrorMapperTest {

    private static final String DEVICE = "dev-123";

    private static SmartThingsError http(SmartThingsOperation op, String deviceId, int status, String body) {
        return SmartThingsErrorMapper.fromHttp(op, deviceId, status, body, null);
    }

    private static String error(String code, String message) {
        return "{\"requestId\":\"req-42\",\"error\":{\"code\":\"" + code + "\",\"message\":\"" + message + "\"}}";
    }

    // --- status HTTP

    @Test
    void unauthorizedBecomesSmartThingsUnauthorized() {
        SmartThingsError e = http(SmartThingsOperation.READ, null, 401, error("Unauthorized", "Invalid token"));
        assertEquals(ApiErrorCode.SMARTTHINGS_UNAUTHORIZED, e.code());
        assertEquals(ErrorSource.SMARTTHINGS, e.source());
        assertFalse(e.retryable());
    }

    @Test
    void forbiddenBecomesSmartThingsForbidden() {
        SmartThingsError e = http(SmartThingsOperation.READ, null, 403, error("Forbidden", "Missing scope"));
        assertEquals(ApiErrorCode.SMARTTHINGS_FORBIDDEN, e.code());
        assertFalse(e.retryable());
    }

    @Test
    void notFoundOnDeviceBecomesDeviceNotFound() {
        SmartThingsError e = http(SmartThingsOperation.READ, DEVICE, 404, error("NotFound", "Device not found"));
        assertEquals(ApiErrorCode.SMARTTHINGS_DEVICE_NOT_FOUND, e.code());
        assertEquals(DEVICE, e.deviceId());
        assertFalse(e.retryable());
    }

    @Test
    void notFoundWithoutDeviceIsGenericSmartThingsError() {
        SmartThingsError e = http(SmartThingsOperation.SCENE, null, 404, error("NotFound", "Scene not found"));
        assertEquals(ApiErrorCode.SMARTTHINGS_ERROR, e.code());
    }

    @Test
    void rateLimitKeepsRetryAfter() {
        SmartThingsError e = SmartThingsErrorMapper.fromHttp(SmartThingsOperation.READ, null, 429, "", 30L);
        assertEquals(ApiErrorCode.SMARTTHINGS_RATE_LIMITED, e.code());
        assertTrue(e.retryable());
        assertEquals(30L, e.retryAfterSeconds());
    }

    @Test
    void serverErrorsBecomeUnavailableAndRetryable() {
        for (int status : new int[]{500, 502, 503}) {
            SmartThingsError e = http(SmartThingsOperation.READ, null, status, "");
            assertEquals(ApiErrorCode.SMARTTHINGS_UNAVAILABLE, e.code(), "HTTP " + status);
            assertTrue(e.retryable(), "HTTP " + status);
        }
    }

    // --- offline: solo con evidenza dal body, mai dallo status

    @Test
    void offlineIsRecognizedFromBodyOnDevice() {
        SmartThingsError e = http(SmartThingsOperation.COMMAND, DEVICE, 409,
                error("ConflictError", "The device is offline"));
        assertEquals(ApiErrorCode.SMARTTHINGS_DEVICE_OFFLINE, e.code());
        assertEquals(424, e.httpStatus());
        assertTrue(e.retryable());
    }

    @Test
    void conflictWithoutOfflineEvidenceIsNotOffline() {
        SmartThingsError e = http(SmartThingsOperation.COMMAND, DEVICE, 409, "");
        assertEquals(ApiErrorCode.SMARTTHINGS_COMMAND_FAILED, e.code());
    }

    // --- comandi

    @Test
    void rejectedCommandIsCommandFailedWithUpstreamCode() {
        SmartThingsError e = http(SmartThingsOperation.COMMAND, DEVICE, 422,
                error("ConstraintViolationError", "Invalid argument for setLevel"));
        assertEquals(ApiErrorCode.SMARTTHINGS_COMMAND_FAILED, e.code());
        assertEquals("ConstraintViolationError", e.upstreamCode());
        assertEquals("req-42", e.upstreamRequestId());
        assertEquals(422, e.upstreamStatus());
        assertEquals(DEVICE, e.deviceId());
    }

    @Test
    void unsupportedCommandIsDetectedFromBody() {
        SmartThingsError e = http(SmartThingsOperation.COMMAND, DEVICE, 422,
                error("UnsupportedCommand", "The command is not supported by this device"));
        assertEquals(ApiErrorCode.SMARTTHINGS_COMMAND_NOT_SUPPORTED, e.code());
    }

    @Test
    void unsupportedCapabilityIsDetectedFromBody() {
        SmartThingsError e = http(SmartThingsOperation.COMMAND, DEVICE, 422,
                error("UnsupportedCapability", "Capability not supported"));
        assertEquals(ApiErrorCode.SMARTTHINGS_CAPABILITY_NOT_SUPPORTED, e.code());
    }

    @Test
    void italianCommandNotSupportedIsRecognized() {
        SmartThingsError e = http(SmartThingsOperation.COMMAND, DEVICE, 422,
                error("Error", "Comando non supportato dal dispositivo"));
        assertEquals(ApiErrorCode.SMARTTHINGS_COMMAND_NOT_SUPPORTED, e.code());
    }

    // --- rete, timeout, risposta non valida

    @Test
    void timeoutFromExceptionChainIsTimeout() {
        SmartThingsError e = SmartThingsErrorMapper.fromException(SmartThingsOperation.READ, null,
                new IllegalStateException(new TimeoutException("slow")));
        assertEquals(ApiErrorCode.SMARTTHINGS_TIMEOUT, e.code());
        assertEquals(ErrorSource.NETWORK, e.source());
        assertTrue(e.retryable());
    }

    @Test
    void networkExceptionIsNetworkError() {
        WebClientRequestException cause = new WebClientRequestException(new IOException("connection reset"),
                HttpMethod.GET, URI.create("https://api.smartthings.com/v1/devices?token=abc"), HttpHeaders.EMPTY);
        SmartThingsError e = SmartThingsErrorMapper.fromException(SmartThingsOperation.READ, null, cause);
        assertEquals(ApiErrorCode.SMARTTHINGS_NETWORK_ERROR, e.code());
        assertEquals(ErrorSource.NETWORK, e.source());
        assertTrue(e.retryable());
        assertFalse(e.detail().contains("abc"), "il token nella URL non deve comparire");
    }

    @Test
    void undecodableResponseIsBadResponse() {
        SmartThingsError e = SmartThingsErrorMapper.fromException(SmartThingsOperation.READ, null,
                new DecodingException("Cannot decode JSON"));
        assertEquals(ApiErrorCode.SMARTTHINGS_BAD_RESPONSE, e.code());
        assertFalse(e.retryable());
    }

    @Test
    void responseExceptionIsClassifiedThroughItsBody() {
        WebClientResponseException ex = WebClientResponseException.create(429, "Too Many Requests",
                HttpHeaders.EMPTY, new byte[0], StandardCharsets.UTF_8);
        SmartThingsError e = SmartThingsErrorMapper.fromException(SmartThingsOperation.READ, null, ex);
        assertEquals(ApiErrorCode.SMARTTHINGS_RATE_LIMITED, e.code());
    }

    @Test
    void authOperationWithInvalidGrantIsUnauthorized() {
        SmartThingsError e = SmartThingsErrorMapper.fromHttp(SmartThingsOperation.AUTH, null, 400,
                error("invalid_grant", "Refresh token expired"), null);
        assertEquals(ApiErrorCode.SMARTTHINGS_UNAUTHORIZED, e.code());
    }

    // --- sicurezza: nessun segreto nel dettaglio, nessun body grezzo

    @Test
    void secretsInUpstreamMessageAreRedacted() {
        String body = error("Error", "bad request, access_token=eyJhbGciOi.secret123 Authorization: Bearer abc.def.ghi "
                + "see https://api.smartthings.com/v1/devices?access_token=xyz");
        SmartThingsError e = http(SmartThingsOperation.COMMAND, DEVICE, 422, body);
        String detail = e.detail();
        assertFalse(detail.contains("eyJhbGciOi"));
        assertFalse(detail.contains("secret123"));
        assertFalse(detail.contains("abc.def.ghi"));
        assertFalse(detail.contains("xyz"));
        assertFalse(detail.contains("https://"));
    }

    @Test
    void detailIsTruncated() {
        String longMessage = "x".repeat(1000);
        SmartThingsError e = http(SmartThingsOperation.READ, null, 500, error("Error", longMessage));
        assertTrue(e.detail().length() <= 301);
    }

    @Test
    void nonJsonBodyFallsBackToStatusWithoutLeakingHtml() {
        SmartThingsError e = http(SmartThingsOperation.READ, null, 502, "<html><body>Bad gateway</body></html>");
        assertEquals(ApiErrorCode.SMARTTHINGS_UNAVAILABLE, e.code());
        assertEquals("SmartThings ha risposto con HTTP 502", e.detail());
        assertNull(e.upstreamCode());
    }

    @Test
    void unknownSmartThingsErrorIsGenericWithDetailPreserved() {
        SmartThingsError e = http(SmartThingsOperation.READ, null, 418, error("TeapotError", "Strange upstream"));
        assertEquals(ApiErrorCode.SMARTTHINGS_ERROR, e.code());
        assertEquals("Strange upstream", e.detail());
        assertEquals("TeapotError", e.upstreamCode());
    }

    @Test
    void userMessageNeverEqualsRawUpstreamText() {
        SmartThingsError e = http(SmartThingsOperation.COMMAND, DEVICE, 422, error("X", "internal raw text"));
        assertEquals(ApiErrorCode.SMARTTHINGS_COMMAND_FAILED.defaultMessage(), e.message());
    }
}
