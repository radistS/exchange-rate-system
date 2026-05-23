package com.marcura.exchangerate.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marcura.exchangerate.repository.ExchangeRateUpsertRepository;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Seeds demo Fixer-style rates from {@code data/sample-rates.json} when live Fixer is unavailable. */
@Service
@RequiredArgsConstructor
@Slf4j
public class SampleRateSeedService {

    private static final MathContext CONTEXT = MathContext.DECIMAL64;
    private static final String SAMPLE_RATES_PATH = "data/sample-rates.json";

    private final ObjectMapper objectMapper;
    private final RateToUsdNormalizer normalizer;
    private final ExchangeRateUpsertRepository upsertRepository;

    /** Inserts missing rows for the last {@code days} calendar days (inclusive of today). */
    public int seedLastDays(int days) {
        SampleRatesFile file = loadSampleRates();
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(days - 1L);
        int total = 0;
        int dayIndex = 0;
        for (LocalDate date = from; !date.isAfter(today); date = date.plusDays(1), dayIndex++) {
            total += seedDate(file, date, dayIndex);
        }
        return total;
    }

    @Transactional
    public int seedDate(SampleRatesFile file, LocalDate date, int dayIndex) {
        Map<String, BigDecimal> fixerRates = applyDailyDrift(file.rates(), dayIndex);
        Map<String, BigDecimal> normalized = normalizer.normalize(file.base(), fixerRates);
        int inserted = 0;
        for (Map.Entry<String, BigDecimal> entry : normalized.entrySet()) {
            inserted += upsertRepository.insertIfAbsent(entry.getKey(), entry.getValue(), date);
        }
        if (inserted > 0) {
            log.info("Sample seed: inserted {} rate rows for {}", inserted, date);
        }
        return inserted;
    }

    SampleRatesFile loadSampleRates() {
        try (InputStream input = new ClassPathResource(SAMPLE_RATES_PATH).getInputStream()) {
            Map<String, Object> raw = objectMapper.readValue(input, new TypeReference<>() {});
            String base = (String) raw.get("base");
            @SuppressWarnings("unchecked")
            Map<String, String> ratesRaw = (Map<String, String>) raw.get("rates");
            Map<String, BigDecimal> rates = new java.util.LinkedHashMap<>();
            ratesRaw.forEach((code, value) -> rates.put(code.toUpperCase(), new BigDecimal(value)));
            return new SampleRatesFile(base, rates);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load " + SAMPLE_RATES_PATH, ex);
        }
    }

    /** Small drift so historical charts and AI insight show movement across the month. */
    Map<String, BigDecimal> applyDailyDrift(Map<String, BigDecimal> baseRates, int dayIndex) {
        BigDecimal factor = BigDecimal.ONE.add(
                new BigDecimal("0.0005").multiply(BigDecimal.valueOf(dayIndex), CONTEXT), CONTEXT);
        Map<String, BigDecimal> drifted = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, BigDecimal> entry : baseRates.entrySet()) {
            if ("EUR".equalsIgnoreCase(entry.getKey())) {
                drifted.put(entry.getKey(), entry.getValue());
            } else {
                drifted.put(entry.getKey(), entry.getValue().multiply(factor, CONTEXT));
            }
        }
        return drifted;
    }

    record SampleRatesFile(String base, Map<String, BigDecimal> rates) {}
}
