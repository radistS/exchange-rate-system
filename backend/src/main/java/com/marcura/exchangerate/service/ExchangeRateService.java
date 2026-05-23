package com.marcura.exchangerate.service;

import com.marcura.exchangerate.domain.CurrencyUsage;
import com.marcura.exchangerate.domain.ExchangeRate;
import com.marcura.exchangerate.repository.CurrencyUsageRepository;
import com.marcura.exchangerate.repository.ExchangeRateRepository;
import com.marcura.exchangerate.service.exception.RateNotFoundException;
import com.marcura.exchangerate.web.dto.ExchangeResponse;
import com.marcura.exchangerate.web.dto.HistoryResponse;
import com.marcura.exchangerate.web.dto.RatePointDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Loads stored rates, applies {@link RateCalculationService}, and records usage. */
@Service
@RequiredArgsConstructor
public class ExchangeRateService {

    private final ExchangeRateRepository exchangeRateRepository;
    private final CurrencyUsageRepository currencyUsageRepository;
    private final RateCalculationService rateCalculationService;

    /** Increments usage counters atomically in the same transaction as the rate lookup. */
    @Transactional
    public ExchangeResponse getExchange(String from, String to, LocalDate requestedDate) {
        String fromCurrency = from.toUpperCase();
        String toCurrency = to.toUpperCase();
        LocalDate rateDate = resolveRateDate(requestedDate);

        BigDecimal fromRate = loadRate(fromCurrency, rateDate);
        BigDecimal toRate = loadRate(toCurrency, rateDate);
        BigDecimal exchange = rateCalculationService.compute(fromRate, toRate, fromCurrency, toCurrency);

        currencyUsageRepository.upsertIncrement(fromCurrency, rateDate);
        currencyUsageRepository.upsertIncrement(toCurrency, rateDate);

        return new ExchangeResponse(
                fromCurrency,
                toCurrency,
                exchange,
                rateDate,
                queryCount(fromCurrency),
                queryCount(toCurrency));
    }

    /**
     * Shared adjusted-rate series for history chart and AI insight; keep all spread logic here.
     */
    @Transactional(readOnly = true)
    public HistoryResponse historicalAdjusted(String from, String to, LocalDate fromDate, LocalDate toDate) {
        String fromCurrency = from.toUpperCase();
        String toCurrency = to.toUpperCase();
        List<LocalDate> dates = exchangeRateRepository.findDistinctRateDatesBetween(fromDate, toDate);
        List<RatePointDto> points = new ArrayList<>();
        for (LocalDate date : dates) {
            if (exchangeRateRepository.findByCurrencyCodeAndRateDate(fromCurrency, date).isEmpty()
                    || exchangeRateRepository.findByCurrencyCodeAndRateDate(toCurrency, date).isEmpty()) {
                continue;
            }
            BigDecimal fromRate = loadRate(fromCurrency, date);
            BigDecimal toRate = loadRate(toCurrency, date);
            BigDecimal rate = rateCalculationService.compute(fromRate, toRate, fromCurrency, toCurrency);
            points.add(new RatePointDto(date, rate));
        }
        return new HistoryResponse(fromCurrency, toCurrency, points);
    }

    /** Uses latest stored rate date when the client omits {@code date}. */
    private LocalDate resolveRateDate(LocalDate requestedDate) {
        if (requestedDate != null) {
            return requestedDate;
        }
        return exchangeRateRepository.findLatestRateDate()
                .orElseThrow(() -> new RateNotFoundException("No exchange rates available in the database"));
    }

    private BigDecimal loadRate(String currency, LocalDate date) {
        return exchangeRateRepository.findByCurrencyCodeAndRateDate(currency, date)
                .map(ExchangeRate::getRateToUsd)
                .orElseThrow(() -> new RateNotFoundException(currency, date));
    }

    private long queryCount(String currency) {
        return currencyUsageRepository.findById(currency).map(CurrencyUsage::getQueryCount).orElse(0L);
    }
}
