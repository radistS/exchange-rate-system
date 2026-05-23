package com.marcura.exchangerate.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Idempotent persistence; {@code rateDate} must be the Fixer API date, not today. */
@Repository
@RequiredArgsConstructor
public class ExchangeRateUpsertRepository {

    private final JdbcTemplate jdbcTemplate;

    public void upsert(String currencyCode, BigDecimal rateToUsd, LocalDate rateDate) {
        jdbcTemplate.update("""
                INSERT INTO exchange_rate (currency_code, rate_to_usd, rate_date)
                VALUES (?, ?, ?)
                ON CONFLICT (currency_code, rate_date)
                DO UPDATE SET rate_to_usd = EXCLUDED.rate_to_usd
                """, currencyCode, rateToUsd, rateDate);
    }

    /** Inserts only when (currency, date) is missing; returns 1 if inserted, 0 if already present. */
    public int insertIfAbsent(String currencyCode, BigDecimal rateToUsd, LocalDate rateDate) {
        return jdbcTemplate.update("""
                INSERT INTO exchange_rate (currency_code, rate_to_usd, rate_date)
                VALUES (?, ?, ?)
                ON CONFLICT (currency_code, rate_date) DO NOTHING
                """, currencyCode, rateToUsd, rateDate);
    }
}
