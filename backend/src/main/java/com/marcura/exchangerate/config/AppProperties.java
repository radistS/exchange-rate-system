package com.marcura.exchangerate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String corsAllowedOrigins, boolean sampleRatesEnabled, int sampleRatesDays) {
}
