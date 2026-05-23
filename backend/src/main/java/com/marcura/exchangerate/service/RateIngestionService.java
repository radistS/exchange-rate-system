package com.marcura.exchangerate.service;

import com.marcura.exchangerate.client.FixerClient;
import com.marcura.exchangerate.client.FixerResponse;
import com.marcura.exchangerate.repository.ExchangeRateRepository;
import com.marcura.exchangerate.repository.ExchangeRateUpsertRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Fetches Fixer and upserts all currencies for the API-reported {@code date}. */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateIngestionService {

    private final FixerClient fixerClient;
    private final RateToUsdNormalizer normalizer;
    private final ExchangeRateUpsertRepository upsertRepository;
    private final ExchangeRateRepository exchangeRateRepository;

    @Transactional
    public int ingestLatest() {
        return ingestResponse(fixerClient.fetchLatest(), true);
    }

    @Transactional
    public int ingestForDate(LocalDate date) {
        return ingestResponse(fixerClient.fetchForDate(date), true);
    }

    /** Fetches latest Fixer snapshot and inserts only rows that are not already stored. */
    @Transactional
    public int ingestLatestIfAbsent() {
        return ingestResponse(fixerClient.fetchLatest(), false);
    }

    /** One Fixer call per calendar day; skips days that fail (weekends, plan limits). */
    public int ingestRange(LocalDate from, LocalDate to) {
        return ingestRangeInternal(from, to, true);
    }

    /** Backfills missing dates only; existing dates and rows are left unchanged. */
    public int ingestMissingRange(LocalDate from, LocalDate to) {
        return ingestRangeInternal(from, to, false);
    }

    private int ingestRangeInternal(LocalDate from, LocalDate to, boolean overwrite) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("from must be on or before to");
        }
        int total = 0;
        for (LocalDate date = from; !date.isAfter(to); date = date.plusDays(1)) {
            if (!overwrite && exchangeRateRepository.existsByRateDate(date)) {
                log.debug("Rates already present for {}, skipping Fixer call", date);
                continue;
            }
            try {
                total += ingestForDateInternal(date, overwrite);
            } catch (Exception ex) {
                log.warn("Skipping ingestion for {}: {}", date, ex.getMessage());
            }
        }
        return total;
    }

    private int ingestForDateInternal(LocalDate date, boolean overwrite) {
        return ingestResponse(fixerClient.fetchForDate(date), overwrite);
    }

    private int ingestResponse(FixerResponse response, boolean overwrite) {
        LocalDate rateDate = response.date(); // must not use LocalDate.now()
        Map<String, BigDecimal> normalized = normalizer.normalize(response.base(), response.rates());
        int count = 0;
        for (Map.Entry<String, BigDecimal> entry : normalized.entrySet()) {
            if (overwrite) {
                upsertRepository.upsert(entry.getKey(), entry.getValue(), rateDate);
                count++;
            } else {
                count += upsertRepository.insertIfAbsent(entry.getKey(), entry.getValue(), rateDate);
            }
        }
        log.info(
                "{} {} rate rows for date {}",
                overwrite ? "Upserted" : "Inserted",
                count,
                rateDate);
        return count;
    }
}
