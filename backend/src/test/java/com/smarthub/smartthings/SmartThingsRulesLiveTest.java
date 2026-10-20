package com.smarthub.smartthings;

import com.smarthub.config.SmartThingsProperties;
import com.smarthub.error.ApiErrorCode;
import com.smarthub.smartthings.api.SmartThingsException;
import com.smarthub.smartthings.api.StRule;
import com.smarthub.smartthings.live.LiveSmartThingsApi;
import com.smarthub.smartthings.live.StaticTokenProvider;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Endpoint delle regole sul client live: percorso, query, paginazione e assenza di retry sull'esecuzione. */
class SmartThingsRulesLiveTest {

    private static final String LOCATION = "loc-1";

    /** Client con retry attivi: le letture possono ritentare, l'esecuzione no. */
    private static LiveSmartThingsApi api(WebClient webClient) {
        SmartThingsProperties props = new SmartThingsProperties();
        props.setMaxRetries(2);
        props.setRequestTimeout(Duration.ofSeconds(2));
        props.setRetryBackoff(Duration.ofMillis(1));
        return new LiveSmartThingsApi(webClient, new StaticTokenProvider("pat-test-token"), props);
    }

    /** Registra ogni richiesta ricevuta e risponde sempre con status e body indicati. */
    private static WebClient recording(List<ClientRequest> requests, HttpStatus status, String body) {
        return WebClient.builder().baseUrl("https://api.smartthings.test/v1").exchangeFunction(request -> {
            requests.add(request);
            return Mono.just(ClientResponse.create(status)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .body(body)
                    .build());
        }).build();
    }

    @Test
    void listRulesReadsItemsFromRulesEndpointWithLocation() {
        List<ClientRequest> requests = new ArrayList<>();
        String body = """
                {"items":[{"id":"rule-1","name":"Luce corridoio","locationId":"loc-1","actions":[]}],"_links":{}}
                """;
        LiveSmartThingsApi api = api(recording(requests, HttpStatus.OK, body));

        List<StRule> rules = api.listRules(LOCATION);

        assertEquals(List.of(new StRule("rule-1", "Luce corridoio")), rules);
        assertEquals(1, requests.size());
        assertEquals("/v1/rules", requests.get(0).url().getPath());
        assertEquals("locationId=loc-1", requests.get(0).url().getQuery());
    }

    @Test
    void listRulesServerErrorIsClassifiedAsSmartThingsError() {
        List<ClientRequest> requests = new ArrayList<>();
        LiveSmartThingsApi api = api(recording(requests, HttpStatus.INTERNAL_SERVER_ERROR, "{}"));

        SmartThingsException e = assertThrows(SmartThingsException.class, () -> api.listRules(LOCATION));

        assertEquals(ApiErrorCode.SMARTTHINGS_UNAVAILABLE, e.error().code());
        // le letture ritentano sui 5xx: più di una richiesta è atteso
        assertTrue(requests.size() > 1);
    }

    @Test
    void executeRuleCallsExecuteEndpointWithLocationOnce() {
        List<ClientRequest> requests = new ArrayList<>();
        LiveSmartThingsApi api = api(recording(requests, HttpStatus.OK, ""));

        assertDoesNotThrow(() -> api.executeRule("rule-1", LOCATION));

        assertEquals(1, requests.size());
        assertEquals(HttpMethod.POST, requests.get(0).method());
        assertEquals("/v1/rules/rule-1/execute", requests.get(0).url().getPath());
        assertEquals("locationId=loc-1", requests.get(0).url().getQuery());
    }

    @Test
    void executeRuleFailureIsNotRetried() {
        List<ClientRequest> requests = new ArrayList<>();
        LiveSmartThingsApi api = api(recording(requests, HttpStatus.INTERNAL_SERVER_ERROR, "{}"));

        SmartThingsException e = assertThrows(SmartThingsException.class, () -> api.executeRule("rule-1", LOCATION));

        assertEquals(ApiErrorCode.SMARTTHINGS_UNAVAILABLE, e.error().code());
        // un'automazione eseguita due volte potrebbe agire due volte sulla casa
        assertEquals(1, requests.size());
    }
}
