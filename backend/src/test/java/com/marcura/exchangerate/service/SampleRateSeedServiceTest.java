package com.marcura.exchangerate.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SampleRateSeedServiceTest {

    private SampleRateSeedService service;

    @BeforeEach
    void setUp() {
        service = new SampleRateSeedService(new ObjectMapper(), new RateToUsdNormalizer(), null);
    }

    @Test
    void loadsSampleRatesFromClasspath() {
        SampleRateSeedService.SampleRatesFile file = service.loadSampleRates();
        assertThat(file.base()).isEqualTo("EUR");
        assertThat(file.rates()).containsKeys("EUR", "USD", "PLN", "JPY");
        assertThat(file.rates().get("USD")).isEqualByComparingTo("1.08");
    }

    @Test
    void driftIncreasesNonBaseRatesOverTime() {
        var file = service.loadSampleRates();
        var day0 = service.applyDailyDrift(file.rates(), 0);
        var day10 = service.applyDailyDrift(file.rates(), 10);
        assertThat(day0.get("EUR")).isEqualByComparingTo("1");
        assertThat(day10.get("PLN")).isGreaterThan(day0.get("PLN"));
    }
}
