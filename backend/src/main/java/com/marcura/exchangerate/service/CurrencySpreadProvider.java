package com.marcura.exchangerate.service;

import com.marcura.exchangerate.config.FixerProperties;
import java.math.BigDecimal;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Spread percentages per currency (assessment Appendix B). */
@Component
public class CurrencySpreadProvider {

    private static final BigDecimal DEFAULT_SPREAD = new BigDecimal("2.75");
    private static final BigDecimal SPREAD_325 = new BigDecimal("3.25");
    private static final BigDecimal SPREAD_450 = new BigDecimal("4.50");
    private static final BigDecimal SPREAD_600 = new BigDecimal("6.00");
    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final Set<String> GROUP_325 = Set.of("JPY", "HKD", "KRW");
    private static final Set<String> GROUP_450 = Set.of("MYR", "INR", "MXN");
    private static final Set<String> GROUP_600 = Set.of("RUB", "CNY", "ZAR");

    private final String baseCurrency;

    public CurrencySpreadProvider(FixerProperties fixerProperties) {
        this.baseCurrency = fixerProperties.baseCurrency().toUpperCase();
    }

    /** Spread for one currency; base currency (Fixer base) is always 0%. */
    public BigDecimal spreadFor(String currency) {
        String code = currency.toUpperCase();
        if (code.equals(baseCurrency)) {
            return ZERO;
        }
        if (GROUP_325.contains(code)) {
            return SPREAD_325;
        }
        if (GROUP_450.contains(code)) {
            return SPREAD_450;
        }
        if (GROUP_600.contains(code)) {
            return SPREAD_600;
        }
        return DEFAULT_SPREAD;
    }

    /** Assessment formula uses the higher spread of the two currencies in the pair. */
    public BigDecimal maxSpread(String fromCurrency, String toCurrency) {
        return spreadFor(fromCurrency).max(spreadFor(toCurrency));
    }
}
