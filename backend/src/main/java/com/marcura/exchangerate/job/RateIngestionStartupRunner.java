package com.marcura.exchangerate.job;

import com.marcura.exchangerate.config.FixerProperties;
import com.marcura.exchangerate.service.RateIngestionService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Loads missing Fixer rates after startup without blocking the HTTP port. */
@Component
@ConditionalOnProperty(name = "fixer.startup-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
@Slf4j
public class RateIngestionStartupRunner implements ApplicationRunner {

    private final FixerProperties fixerProperties;
    private final RateIngestionService rateIngestionService;

    @Override
    public void run(ApplicationArguments args) {
        if (!fixerProperties.hasApiKey()) {
            log.info("FIXER_API_KEY not configured — skipping startup Fixer ingestion");
            return;
        }

        Thread.startVirtualThread(this::ingestMissingRates);
    }

    private void ingestMissingRates() {
        try {
            int backfillDays = Math.max(1, fixerProperties.startupBackfillDays());
            LocalDate to = LocalDate.now().minusDays(1);
            LocalDate from = to.minusDays(backfillDays - 1L);

            int backfilled = rateIngestionService.ingestMissingRange(from, to);
            int latest = rateIngestionService.ingestLatestIfAbsent();

            log.info(
                    "Startup Fixer ingestion finished: {} new rate rows for {}–{}, {} new rows for latest",
                    backfilled,
                    from,
                    to,
                    latest);
        } catch (Exception ex) {
            log.warn("Startup Fixer ingestion failed: {}", ex.getMessage());
        }
    }
}
