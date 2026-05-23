package com.marcura.exchangerate.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.springframework.stereotype.Service;

/**
 * Spread-adjusted exchange rate:
 * (toRateToUsd / fromRateToUsd) * ((100 - max(toSpread, fromSpread)) / 100)
 */
@Service
public class RateCalculationService {

    public static final int RESULT_SCALE = 10;
    private static final MathContext DECIMAL64 = MathContext.DECIMAL64;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final CurrencySpreadProvider currencySpreadProvider;

    public RateCalculationService(CurrencySpreadProvider currencySpreadProvider) {
        this.currencySpreadProvider = currencySpreadProvider;
    }

    /**
     * Single implementation of the spread-adjusted formula; do not duplicate elsewhere.
     */
    public BigDecimal compute(
            BigDecimal fromRateToUsd,
            BigDecimal toRateToUsd,
            String fromCurrency,
            String toCurrency) {
        BigDecimal pairRate = toRateToUsd.divide(fromRateToUsd, DECIMAL64);
        BigDecimal spread = currencySpreadProvider.maxSpread(fromCurrency, toCurrency);
        BigDecimal multiplier = HUNDRED.subtract(spread).divide(HUNDRED, DECIMAL64);
        return pairRate.multiply(multiplier, DECIMAL64).setScale(RESULT_SCALE, RoundingMode.HALF_UP);
    }
}
