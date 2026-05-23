package com.marcura.exchangerate.job;

import com.marcura.exchangerate.config.AppProperties;
import com.marcura.exchangerate.config.FixerProperties;
import com.marcura.exchangerate.service.SampleRateSeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/** Loads demo rates from {@code data/sample-rates.json} when Fixer API key is not configured. */
@Component
@ConditionalOnProperty(name = "app.sample-rates-enabled", havingValue = "true", matchIfMissing = true)
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class SampleRateStartupRunner implements ApplicationRunner {

    private final FixerProperties fixerProperties;
    private final AppProperties appProperties;
    private final SampleRateSeedService sampleRateSeedService;

    @Override
    public void run(ApplicationArguments args) {
        if (fixerProperties.hasApiKey()) {
            log.debug("Fixer API key present — sample rate seed skipped");
            return;
        }

        Thread.startVirtualThread(() -> {
            try {
                int days = Math.max(1, appProperties.sampleRatesDays());
                int inserted = sampleRateSeedService.seedLastDays(days);
                log.info("Sample rate seed finished: {} new rows for last {} days", inserted, days);
            } catch (Exception ex) {
                log.warn("Sample rate seed failed: {}", ex.getMessage());
            }
        });
    }
}
