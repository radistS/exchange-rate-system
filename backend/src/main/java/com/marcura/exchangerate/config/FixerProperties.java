package com.marcura.exchangerate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fixer")
public record FixerProperties(
        String apiKey,
        String baseUrl,
        String baseCurrency,
        boolean startupEnabled,
        int startupBackfillDays) {

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }
}
