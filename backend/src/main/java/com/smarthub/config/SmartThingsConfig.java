package com.smarthub.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarthub.smartthings.api.SmartThingsApi;
import com.smarthub.smartthings.live.LiveSmartThingsApi;
import com.smarthub.smartthings.live.OAuthTokenProvider;
import com.smarthub.smartthings.live.StaticTokenProvider;
import com.smarthub.smartthings.live.TokenProvider;
import com.smarthub.smartthings.mapping.CommandTranslator;
import com.smarthub.smartthings.mapping.DeviceNormalizer;
import com.smarthub.smartthings.mock.MockSmartThingsApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Wiring dell'integrazione SmartThings: in base a SMARTTHINGS_MODE viene creata
 * l'implementazione mock oppure quella live.
 */
@Configuration
public class SmartThingsConfig {

    private static final Logger log = LoggerFactory.getLogger(SmartThingsConfig.class);

    @Bean
    public WebClient smartThingsWebClient(WebClient.Builder builder, SmartThingsProperties properties) {
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(properties.getRequestTimeout());
        return builder
                .baseUrl(properties.getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Bean
    public TokenProvider tokenProvider(SmartThingsProperties properties, WebClient smartThingsWebClient,
                                       ObjectMapper objectMapper) {
        if (properties.getAuth() == SmartThingsProperties.AuthType.OAUTH) {
            return new OAuthTokenProvider(properties.getOauth(), smartThingsWebClient, objectMapper,
                    properties.getRequestTimeout());
        }
        return new StaticTokenProvider(properties.getToken());
    }

    @Bean
    public SmartThingsApi smartThingsApi(SmartThingsProperties properties, WebClient smartThingsWebClient,
                                         TokenProvider tokenProvider) {
        if (properties.isMock()) {
            return new MockSmartThingsApi(properties.getMock());
        }
        if (!tokenProvider.isConfigured()) {
            throw new IllegalStateException(properties.getAuth() == SmartThingsProperties.AuthType.OAUTH
                    ? "SMARTTHINGS_MODE=live con auth OAuth: impostare SMARTTHINGS_OAUTH_CLIENT_ID e SMARTTHINGS_OAUTH_CLIENT_SECRET"
                    : "SMARTTHINGS_MODE=live: impostare SMARTTHINGS_TOKEN (oppure usare SMARTTHINGS_MODE=mock)");
        }
        log.info("SmartThings in modalità LIVE ({}), base URL {}", tokenProvider.describe(), properties.getBaseUrl());
        return new LiveSmartThingsApi(smartThingsWebClient, tokenProvider, properties);
    }

    @Bean
    public DeviceNormalizer deviceNormalizer(SmartThingsProperties properties) {
        return new DeviceNormalizer(properties.getDeviceTypeOverrides());
    }

    @Bean
    public CommandTranslator commandTranslator() {
        return new CommandTranslator();
    }
}
