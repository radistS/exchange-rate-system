package com.marcura.exchangerate.client;

import com.marcura.exchangerate.config.FixerProperties;
import com.marcura.exchangerate.service.exception.FixerApiException;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

/** HTTP client for Fixer.io; {@link FixerResponse#date()} drives stored {@code rate_date}. */
@Component
@RequiredArgsConstructor
@Slf4j
public class FixerClient {

    private final RestClient restClient;
    private final FixerProperties properties;

    public FixerResponse fetchLatest() {
        return fetch("latest");
    }

    /** Historical rates for a calendar day — requires a Fixer plan with historical access. */
    public FixerResponse fetchForDate(LocalDate date) {
        return fetch(date.toString());
    }

    private FixerResponse fetch(String pathSegment) {
        if (!properties.hasApiKey()) {
            throw new FixerApiException("FIXER_API_KEY is not configured");
        }
        String uri = UriComponentsBuilder.fromHttpUrl(properties.baseUrl() + "/" + pathSegment)
                .queryParam("access_key", properties.apiKey())
                .queryParam("base", properties.baseCurrency())
                .toUriString();
        try {
            FixerResponse response = restClient.get().uri(uri).retrieve().body(FixerResponse.class);
            if (response == null) {
                throw new FixerApiException("Empty response from Fixer.io");
            }
            if (!response.success()) {
                String message = response.error() != null ? response.error().info() : "Fixer.io request failed";
                log.warn("Fixer.io returned failure for {}: {}", pathSegment, message);
                throw new FixerApiException(message);
            }
            if (response.rates() == null || response.rates().isEmpty() || response.date() == null) {
                throw new FixerApiException("Fixer.io response missing rates or date");
            }
            return response;
        } catch (FixerApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Fixer.io call failed for {}", pathSegment, ex);
            throw new FixerApiException("Failed to call Fixer.io", ex);
        }
    }
}
