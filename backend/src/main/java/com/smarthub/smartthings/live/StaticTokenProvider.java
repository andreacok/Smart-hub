package com.smarthub.smartthings.live;

import com.smarthub.smartthings.api.SmartThingsException;

/** Personal Access Token configurato via SMARTTHINGS_TOKEN. */
public class StaticTokenProvider implements TokenProvider {

    private final String token;

    public StaticTokenProvider(String token) {
        this.token = token == null ? "" : token.trim();
    }

    @Override
    public String accessToken() {
        if (token.isEmpty()) {
            throw new SmartThingsException("SMARTTHINGS_TOKEN non configurato", 401, null);
        }
        return token;
    }

    @Override
    public boolean isConfigured() {
        return !token.isEmpty();
    }

    @Override
    public String describe() {
        return "Personal Access Token" + (isConfigured() ? "" : " (mancante)");
    }
}
