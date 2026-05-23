package com.marcura.exchangerate.job;

import com.marcura.exchangerate.service.RateIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Daily Fixer pull at 12:05 AM GMT; ShedLock avoids duplicate runs in multi-instance deploys. */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateIngestionScheduler {

    private final RateIngestionService rateIngestionService;

    @Scheduled(cron = "0 5 0 * * *", zone = "GMT")
    @SchedulerLock(name = "fixer-daily-ingest", lockAtMostFor = "30m", lockAtLeastFor = "1m")
    public void ingestDailyRates() {
        log.info("Starting scheduled Fixer.io ingestion");
        int count = rateIngestionService.ingestLatest();
        log.info("Scheduled ingestion finished, {} rates upserted", count);
    }
}
