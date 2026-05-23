package com.marcura.exchangerate.repository;

import com.marcura.exchangerate.domain.CurrencyUsage;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CurrencyUsageRepository extends JpaRepository<CurrencyUsage, String> {

    /**
     * Atomic increment safe under concurrent requests and multiple app instances (Postgres row lock).
     * Do not replace with read-modify-write in Java.
     */
    @Modifying
    @Query(value = """
            INSERT INTO currency_usage (currency_code, query_count, last_queried_at)
            VALUES (:currency, 1, :queriedDate)
            ON CONFLICT (currency_code) DO UPDATE SET
                query_count = currency_usage.query_count + 1,
                last_queried_at = GREATEST(
                    COALESCE(currency_usage.last_queried_at, EXCLUDED.last_queried_at),
                    EXCLUDED.last_queried_at)
            """, nativeQuery = true)
    void upsertIncrement(@Param("currency") String currency, @Param("queriedDate") LocalDate queriedDate);

    List<CurrencyUsage> findAllByOrderByQueryCountDesc();
}
