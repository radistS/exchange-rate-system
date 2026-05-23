package com.marcura.exchangerate.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.marcura.exchangerate.config.FixerProperties;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CurrencySpreadProviderTest {

    @Mock
    private FixerProperties fixerProperties;

    private CurrencySpreadProvider provider;

    @BeforeEach
    void setUp() {
        when(fixerProperties.baseCurrency()).thenReturn("EUR");
        provider = new CurrencySpreadProvider(fixerProperties);
    }

    @Test
    void baseCurrencySpreadIsZero() {
        assertThat(provider.spreadFor("EUR")).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(provider.spreadFor("eur")).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void spread325Group() {
        assertThat(provider.spreadFor("JPY")).isEqualByComparingTo(new BigDecimal("3.25"));
        assertThat(provider.spreadFor("hkd")).isEqualByComparingTo(new BigDecimal("3.25"));
        assertThat(provider.spreadFor("KRW")).isEqualByComparingTo(new BigDecimal("3.25"));
    }

    @Test
    void spread450Group() {
        assertThat(provider.spreadFor("MYR")).isEqualByComparingTo(new BigDecimal("4.50"));
        assertThat(provider.spreadFor("INR")).isEqualByComparingTo(new BigDecimal("4.50"));
        assertThat(provider.spreadFor("MXN")).isEqualByComparingTo(new BigDecimal("4.50"));
    }

    @Test
    void spread600Group() {
        assertThat(provider.spreadFor("RUB")).isEqualByComparingTo(new BigDecimal("6.00"));
        assertThat(provider.spreadFor("CNY")).isEqualByComparingTo(new BigDecimal("6.00"));
        assertThat(provider.spreadFor("ZAR")).isEqualByComparingTo(new BigDecimal("6.00"));
    }

    @Test
    void defaultSpreadForUnlistedCurrencies() {
        assertThat(provider.spreadFor("PLN")).isEqualByComparingTo(new BigDecimal("2.75"));
        assertThat(provider.spreadFor("USD")).isEqualByComparingTo(new BigDecimal("2.75"));
        assertThat(provider.spreadFor("GBP")).isEqualByComparingTo(new BigDecimal("2.75"));
    }

    @Test
    void maxSpreadUsesHigherOfPairNotSum() {
        assertThat(provider.maxSpread("EUR", "PLN")).isEqualByComparingTo(new BigDecimal("2.75"));
        assertThat(provider.maxSpread("JPY", "INR")).isEqualByComparingTo(new BigDecimal("4.50"));
        assertThat(provider.maxSpread("CNY", "JPY")).isEqualByComparingTo(new BigDecimal("6.00"));
    }

    @Test
    void respectsConfiguredBaseCurrency() {
        when(fixerProperties.baseCurrency()).thenReturn("USD");
        provider = new CurrencySpreadProvider(fixerProperties);
        assertThat(provider.spreadFor("USD")).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(provider.spreadFor("EUR")).isEqualByComparingTo(new BigDecimal("2.75"));
    }
}
