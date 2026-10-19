package com.smarthub.smartthings.live;

import java.util.List;

import com.smarthub.smartthings.api.SmartThingsException;
import com.smarthub.smartthings.error.SmartThingsErrorMapper;
import com.smarthub.smartthings.error.SmartThingsOperation;
import com.smarthub.error.ApiErrorCode;

/** Personal Access Token configurato via SMARTTHINGS_TOKEN. */
public class StaticTokenProvider implements TokenProvider {

    private final String token;

    public StaticTokenProvider(String token) {
        this.token = token == null ? "" : token.trim();
    }

    @Override
    public List<String> secrets() {
        return token.isEmpty() ? List.of() : List.of(token);
    }

    @Override
    public String accessToken() {
        if (token.isEmpty()) {
            throw new SmartThingsException(SmartThingsErrorMapper.custom(SmartThingsOperation.AUTH,
                    ApiErrorCode.SMARTTHINGS_UNAUTHORIZED, null, "SMARTTHINGS_TOKEN non configurato"));
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
