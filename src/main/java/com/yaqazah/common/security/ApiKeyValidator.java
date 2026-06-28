package com.yaqazah.common.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApiKeyValidator {

    // This grabs the value straight from application.yml
    @Value("${app.security.api-key}")
    private String expectedApiKey;

    public boolean isValid(String incomingKeyFromFlutter) {
        return expectedApiKey.equals(incomingKeyFromFlutter);
    }
}