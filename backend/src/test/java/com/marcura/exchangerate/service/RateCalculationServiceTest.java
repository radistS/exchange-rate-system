package com.marcura.exchangerate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marcura.exchangerate.config.FixerProperties;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RateCalculationServiceTest {

    private static final BigDecimal EUR_TO_USD = new BigDecimal("0.8");
    private static final BigDecimal PLN_TO_USD = new BigDecimal("3.7");

    @Mock
    private CurrencySpreadProvider currencySpreadProvider;

    @Mock
    private FixerProperties fixerProperties;

    private RateCalculationService rateCalculationService;

    @BeforeEach
    void setUp() {
        rateCalculationService = new RateCalculationService(currencySpreadProvider);
    }

    /** Assessment §6.2: (3.7/0.8) × ((100−4)/100) = 4.44 */
    @Test
    void assessmentEurPlnWorkedExample() {
        when(currencySpreadProvider.maxSpread("EUR", "PLN")).thenReturn(new BigDecimal("4.00"));
        BigDecimal result = rateCalculationService.compute(EUR_TO_USD, PLN_TO_USD, "EUR", "PLN");
        assertThat(result).isEqualByComparingTo(new BigDecimal("4.4400000000"));
    }

    @Test
    void appliesHigherSpreadOfCurrencyPair() {
        when(currencySpreadProvider.maxSpread("JPY", "INR")).thenReturn(new BigDecimal("4.50"));
        BigDecimal result = rateCalculationService.compute(
                new BigDecimal("1.0"), new BigDecimal("130.0"), "JPY", "INR");
        assertThat(result).isEqualByComparingTo(new BigDecimal("124.1500000000"));
    }

    @Test
    void zeroSpreadWhenBothAreBaseCurrency() {
        when(currencySpreadProvider.maxSpread("EUR", "EUR")).thenReturn(BigDecimal.ZERO);
        BigDecimal result = rateCalculationService.compute(
                new BigDecimal("1.1"), new BigDecimal("1.1"), "EUR", "EUR");
        assertThat(result).isEqualByComparingTo(new BigDecimal("1.0000000000"));
    }

    /** Appendix B: PLN 2.75% vs EUR 0% → max 2.75%. */
    @Test
    void computesWithRealSpreadProviderForEurPln() {
        when(fixerProperties.baseCurrency()).thenReturn("EUR");
        RateCalculationService service = new RateCalculationService(new CurrencySpreadProvider(fixerProperties));
        BigDecimal result = service.compute(EUR_TO_USD, PLN_TO_USD, "EUR", "PLN");
        assertThat(result).isEqualByComparingTo(new BigDecimal("4.4978125000"));
    }

    @Test
    void roundsToResultScaleHalfUp() {
        when(currencySpreadProvider.maxSpread("USD", "GBP")).thenReturn(new BigDecimal("2.75"));
        BigDecimal result = rateCalculationService.compute(
                new BigDecimal("1.0"), new BigDecimal("0.7891234567"), "USD", "GBP");
        assertThat(result.scale()).isEqualTo(RateCalculationService.RESULT_SCALE);
        assertThat(result).isEqualByComparingTo(
                new BigDecimal("0.7891234567")
                        .multiply(new BigDecimal("0.9725"), MathContext.DECIMAL64)
                        .setScale(RateCalculationService.RESULT_SCALE, RoundingMode.HALF_UP));
    }
}
