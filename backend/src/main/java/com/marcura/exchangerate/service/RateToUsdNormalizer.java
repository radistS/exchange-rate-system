package com.marcura.exchangerate.service;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Converts Fixer base-relative quotes into USD-per-unit for {@link RateCalculationService}. */
@Component
public class RateToUsdNormalizer {

    private static final MathContext CONTEXT = MathContext.DECIMAL64;
    private static final String USD = "USD";

    /** {@code rateToUsd(X) = usdPerBase / rates[X]} when Fixer base is not USD. */
    public Map<String, BigDecimal> normalize(String apiBase, Map<String, BigDecimal> ratesFromApi) {
        Map<String, BigDecimal> normalized = new HashMap<>();
        BigDecimal usdPerBase = resolveRate(ratesFromApi, USD);

        for (Map.Entry<String, BigDecimal> entry : ratesFromApi.entrySet()) {
            String currency = entry.getKey().toUpperCase();
            if (USD.equals(currency)) {
                normalized.put(USD, BigDecimal.ONE);
                continue;
            }
            BigDecimal rateToUsd = usdPerBase.divide(entry.getValue(), CONTEXT);
            normalized.put(currency, rateToUsd);
        }
        normalized.putIfAbsent(USD, BigDecimal.ONE);
        String base = apiBase.toUpperCase();
        if (!USD.equals(base) && !normalized.containsKey(base)) {
            normalized.put(base, usdPerBase);
        }
        return normalized;
    }

    private BigDecimal resolveRate(Map<String, BigDecimal> rates, String target) {
        BigDecimal rate = rates.get(target);
        if (rate == null) {
            rate = rates.entrySet().stream()
                    .filter(e -> e.getKey().equalsIgnoreCase(target))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse(null);
        }
        if (rate == null || rate.signum() == 0) {
            throw new IllegalArgumentException("Missing or zero USD rate in Fixer response");
        }
        return rate;
    }
}
