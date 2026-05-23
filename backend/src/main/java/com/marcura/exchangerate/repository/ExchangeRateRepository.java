package com.marcura.exchangerate.repository;

import com.marcura.exchangerate.domain.ExchangeRate;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ExchangeRateRepository extends JpaRepository<ExchangeRate, Long> {

    Optional<ExchangeRate> findByCurrencyCodeAndRateDate(String currencyCode, LocalDate rateDate);

    boolean existsByRateDate(LocalDate rateDate);

    @Query("SELECT MAX(e.rateDate) FROM ExchangeRate e")
    Optional<LocalDate> findLatestRateDate();

    @Query("""
            SELECT DISTINCT e.rateDate FROM ExchangeRate e
            WHERE e.rateDate BETWEEN :fromDate AND :toDate
            ORDER BY e.rateDate ASC
            """)
    List<LocalDate> findDistinctRateDatesBetween(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate);
}
